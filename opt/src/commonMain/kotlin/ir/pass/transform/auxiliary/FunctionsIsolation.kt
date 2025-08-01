package ir.pass.transform.auxiliary

import ir.attributes.ByValue
import ir.global.GlobalValue
import ir.types.*
import ir.instruction.*
import ir.instruction.lir.Generate
import ir.instruction.lir.Lea
import ir.instruction.matching.anytype
import ir.instruction.matching.gVisible
import ir.module.FunctionData
import ir.module.Module
import ir.module.SSAModule
import ir.module.block.Block
import ir.pass.CompileContext
import ir.value.*
import ir.value.constant.U64Value


internal class FunctionsIsolation private constructor(private val cfg: FunctionData, private val ctx: CompileContext) {
    private fun isolateByValueArgument(bb: Block, call: Instruction, i: ByValue, arg: Value) {
        call as Callable

        val gen = bb.putBefore(call, Generate.gen(i.aggregateType))
        val lea = bb.putAfter(gen, Lea.lea(gen))
        bb.putAfter(lea, Memcpy.memcpy(lea, arg, U64Value.of(i.aggregateType.sizeOf().toLong())))
        call.arg(i.argumentIndex, gen)
    }

    private fun isolateByValueArguments(bb: Block, call: Instruction) {
        call as Callable
        val byValueAttr = call.attributes().filterIsInstance<ByValue>()
        for (byValue in byValueAttr) {
            val arg = call.arguments()[byValue.argumentIndex]
            isolateByValueArgument(bb, call, byValue, arg)
        }
    }

    private fun insertCopies(bb: Block, call: Instruction) {
        call as Callable
        val byValueAttr = call.attributes().filterIsInstance<ByValue>()
        for ((i, arg) in call.arguments().withIndex()) {
            when (val ty = arg.type()) {
                is FloatingPointType, is IntegerType -> {
                    val copy = bb.putBefore(call, Copy.copy(arg))
                    call.arg(i, copy)
                }
                is PtrType -> {
                    val copyOrLea = if (ctx.pic() && arg.isa(gVisible())) {
                        bb.putBefore(call, Copy.copy(arg))

                    } else {
                        if (arg is GlobalValue) {
                            bb.putBefore(call, Lea.lea(arg))
                        } else {
                            bb.putBefore(call, Copy.copy(arg))
                        }
                    }
                    call.arg(i, copyOrLea)
                }
                is AggregateType -> {
                    val byValue = byValueAttr.find { it.argumentIndex == i }
                    if (byValue == null) {
                        throw IllegalStateException("ByValue attribute not found for argument $i")
                    }
                }
                is UndefType -> {}
                else -> throw IllegalArgumentException("Unexpected type: $ty")
            }
        }
    }

    private fun wrapCallInstruction(bb: Block, call: Instruction): Instruction {
        if (call !is Callable) {
            return call
        }
        bb.putBefore(call, DownStackFrame.dsf(call))
        isolateByValueArguments(bb, call)
        insertCopies(bb, call)
        call.target().prepend(UpStackFrame.usf(call))
        return call
    }

    private fun isolateCall() {
        for (bb in cfg) {
            bb.transform { inst -> wrapCallInstruction(bb, inst) }
        }
    }

    fun pass() {
       // isolateArgumentValues()
        isolateCall()
    }

    companion object {
        fun run(module: SSAModule, ctx: CompileContext): SSAModule {
            module.functions().forEach { FunctionsIsolation(it, ctx).pass() }
            return SSAModule(module.functions, module.externFunctions, module.constantPool, module.globals, module.types)
        }
    }
}