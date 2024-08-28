package ir.instruction

import common.arrayWith
import common.arrayWrapperOf
import common.assertion
import ir.value.Value
import ir.types.Type
import ir.module.IndirectFunctionPrototype
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block

class IndirectionVoidCall private constructor(id: Identity, owner: Block, pointer: Value,
                                              private val func: IndirectFunctionPrototype, args: List<Value>, target: Block):
    TerminateInstruction(id, owner, (args + pointer).toTypedArray(), arrayOf(target)), //TODO
    Callable {
    init {
        assertion(func.returnType() == Type.Void) { "Must be ${Type.Void}" }
    }

    override fun target(): Block {
        assertion(targets.size == 1) {
            "size should be 1 target in $this instruction, but '$targets' found"
        }

        return targets[0]
    }

    fun pointer(): Value {
        assertion(operands.isNotEmpty()) {
            "size should be at least 1 operand in $this instruction"
        }

        return operands[0]
    }

    override fun arguments(): List<Value> {
        return arrayWrapperOf(arrayWith(operands.size - 1) { operands[it + 1] })
    }

    override fun prototype(): IndirectFunctionPrototype {
        return func
    }

    override fun type(): Type = Type.Void

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    override fun dump(): String {
        val builder = StringBuilder()
        builder.append("call ${type()} ${pointer()}(")
        arguments().joinTo(builder) {
            "$it:${it.type()}"
        }
        builder.append(") br label ")
        builder.append(target())
        return builder.toString()
    }

    companion object {
        fun make(id: Identity, owner: Block, pointer: Value, func: IndirectFunctionPrototype, args: List<Value>, block: Block): IndirectionVoidCall {
            require(Callable.isAppropriateTypes(func, pointer, args)) {
                args.joinToString(prefix = "inconsistent types: pointer=${pointer}:${pointer.type()}, prototype='${func.shortName()}', ")
                { "$it: ${it.type()}" }
            }

            val l = listOf(pointer) + args
            return registerUser(IndirectionVoidCall(id, owner, pointer, func, args, block), l.iterator())
        }
    }
}