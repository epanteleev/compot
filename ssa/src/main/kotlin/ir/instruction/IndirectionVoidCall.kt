package ir.instruction

import ir.Value
import ir.types.Type
import ir.AnyFunctionPrototype
import ir.IndirectFunctionPrototype
import ir.instruction.utils.Visitor


class IndirectionVoidCall private constructor(pointer: Value, private val func: IndirectFunctionPrototype, args: List<Value>):
    Instruction((args + pointer).toTypedArray()),
    Callable {
    init {
        assert(func.returnType() == Type.Void) { "Must be ${Type.Void}" }
    }

    fun pointer(): Value {
        assert(operands.size > 1) {
            "size should be at least 1 operand in $this instruction"
        }

        return operands[0]
    }

    override fun arguments(): Array<Value> {
        return operands
    }

    override fun prototype(): AnyFunctionPrototype {
        return func
    }

    override fun type(): Type = Type.Void

    override fun copy(newUsages: List<Value>): IndirectionVoidCall {
        assert(newUsages.size == operands.size) {
            "should be, but newUsages=$newUsages"
        }

        return make(newUsages[0], func, newUsages.takeLast(newUsages.size - 2)) // TODO Creation new list
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IndirectionVoidCall

        if (func != other.func) return false
        return operands.contentEquals(other.operands)
    }

    override fun hashCode(): Int {
        return func.hashCode()
    }

    override fun visit(visitor: Visitor) {
        visitor.visit(this)
    }

    override fun dump(): String {
        val builder = StringBuilder()
        builder.append("call ${type()} ${pointer()}(")
        operands.joinTo(builder) { "$it:${it.type()}"}
        builder.append(")")
        return builder.toString()
    }

    companion object {
        fun make(pointer: Value, func: IndirectFunctionPrototype, args: List<Value>): IndirectionVoidCall {
            require(Callable.isAppropriateTypes(func, pointer, args.toTypedArray())) {
                args.joinToString(prefix = "inconsistent types: pointer=${pointer}, prototype='${func.shortName()}', ")
                { "$it: ${it.type()}" }
            }

            return registerUser(IndirectionVoidCall(pointer, func, args), args.iterator())
        }
    }
}