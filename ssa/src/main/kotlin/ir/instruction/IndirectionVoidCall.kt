package ir.instruction

import ir.Value
import ir.types.Type
import ir.module.IndirectFunctionPrototype
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class IndirectionVoidCall private constructor(id: Identity, owner: Block, pointer: Value,
                                              private val func: IndirectFunctionPrototype, args: List<Value>, target: Block):
    TerminateInstruction(id, owner, (args + pointer).toTypedArray(), arrayOf(target)), //TODO
    Callable {
    init {
        assert(func.returnType() == Type.Void) { "Must be ${Type.Void}" }
    }

    override fun target(): Block {
        assert(targets.size == 1) {
            "size should be 1 target in $this instruction, but '$targets' found"
        }

        return targets[0]
    }

    fun pointer(): Value {
        assert(operands.isNotEmpty()) {
            "size should be at least 1 operand in $this instruction"
        }

        return operands[0]
    }

    override fun arguments(): Array<Value> {
        return operands
    }

    override fun prototype(): IndirectFunctionPrototype {
        return func
    }

    override fun type(): Type = Type.Void

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IndirectionVoidCall

        return func != other.func
    }

    override fun hashCode(): Int {
        return func.hashCode()
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    override fun dump(): String {
        val builder = StringBuilder()
        builder.append("call ${type()} ${pointer()}(")
        operands.joinTo(builder) { "$it:${it.type()}"}
        builder.append(") br label ")
        builder.append(target())
        return builder.toString()
    }

    companion object {
        fun make(id: Identity, owner: Block, pointer: Value, func: IndirectFunctionPrototype, args: List<Value>, block: Block): IndirectionVoidCall {
            require(Callable.isAppropriateTypes(func, pointer, args.toTypedArray())) {
                args.joinToString(prefix = "inconsistent types: pointer=${pointer}:${pointer.type()}, prototype='${func.shortName()}', ")
                { "$it: ${it.type()}" }
            }

            return registerUser(IndirectionVoidCall(id, owner, pointer, func, args, block), args.iterator())
        }
    }
}