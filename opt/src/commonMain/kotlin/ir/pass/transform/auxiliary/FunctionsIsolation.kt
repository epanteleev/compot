package ir.pass.transform.auxiliary

import ir.attributes.ByValue
import ir.global.GlobalValue
import ir.types.*
import ir.instruction.*
import ir.instruction.lir.Generate
import ir.instruction.lir.Lea
import ir.module.FunctionData
import ir.module.Module
import ir.module.SSAModule
import ir.module.block.Block
import ir.pass.analysis.LivenessAnalysisPassFabric
import ir.value.*
import ir.value.constant.U64Value


internal class FunctionsIsolation private constructor(private val cfg: FunctionData) {
    private val liveness = cfg.analysis(LivenessAnalysisPassFabric)
    private val allCalls = run { //TODO separate analysis pass
        val calls = arrayListOf<Instruction>()
        for (bb in cfg) {
            for (inst in bb) {
                if (inst is Callable) {
                    calls.add(inst)
                }
            }
        }
        calls
    }

    private fun liveOut(liveOutWhat: Instruction, arg: ArgumentValue): Boolean {
        if (liveness.liveOut(liveOutWhat.owner()).contains(arg)) {
            // Argument is live out of the call
            return true
        }
        if (liveOutWhat.operands().contains(arg)) {
            // Argument is used in the call
            return true
        }

        return false
    }

    private fun mustBeIsolated(arg: ArgumentValue, index: Int): Boolean {
        if (arg.attributes.find { it is ByValue } != null) {
            // Argument is in overflow area
            return false
        }

        val argType = arg.type()
        if (index == 2 && (argType is IntegerType || argType is PtrType)) {
            /*for (rdxOp in RDXFixedReg) {
                if (liveOut(rdxOp, arg)) {
                    return true
                }
            }*/

            return true
        }

        if (index == 3 && (argType is IntegerType || argType is PtrType)) {
            /*for (rcxOp in RCXFixedReg) {
                if (liveOut(rcxOp, arg)) {
                    return true
                }
            }*/

            return true
        }

        for (call in allCalls) {
            call as Callable
            if (liveOut(call, arg)) {
                return true
            }
        }

        return false
    }

    private fun isolateArgumentValues() {
        val begin = cfg.begin()
        for ((idx, arg) in cfg.arguments().withIndex()) {
            if (!mustBeIsolated(arg, idx)) {
                continue
            }

            begin.updateUsages(arg) { begin.prepend(Copy.copy(arg)) }
        }
    }

    private fun isolateByValueArgument(bb: Block, call: Instruction, i: ByValue, arg: Value) {
        val gen = bb.putBefore(call, Generate.gen(i.aggregateType))
        val lea = bb.putAfter(gen, Lea.lea(gen))
        bb.putAfter(lea, Memcpy.memcpy(lea, arg, U64Value.of(i.aggregateType.sizeOf().toLong())))
        bb.updateDF(call, i.argumentIndex, gen)
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
                    bb.updateDF(call, i, copy)
                }
                is PtrType -> {
                    val copyOrLea = if (arg is GlobalValue) {
                        bb.putBefore(call, Lea.lea(arg))
                    } else {
                        bb.putBefore(call, Copy.copy(arg))
                    }
                    bb.updateDF(call, i, copyOrLea)
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
        isolateArgumentValues()
        isolateCall()
    }

    companion object {
        fun run(module: Module): Module {
            module.functions().forEach { FunctionsIsolation(it).pass() }
            return SSAModule(module.functions, module.externFunctions, module.constantPool, module.globals, module.types)
        }
    }
}