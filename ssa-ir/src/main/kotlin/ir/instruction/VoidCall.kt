package ir.instruction

import ir.Value
import ir.types.Type
import ir.AnyFunctionPrototype

class VoidCall(private val func: AnyFunctionPrototype, args: List<Value>):
    Instruction(Type.Void, args.toTypedArray()),
    Callable {
    init {
        require(func.type() == Type.Void) { "Must be void" }
    }

    override fun arguments(): Array<Value> {
        return operands
    }

    override fun prototype(): AnyFunctionPrototype {
        return func
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VoidCall

        if (func != other.func) return false
        return operands.contentEquals(other.operands)
    }

    override fun hashCode(): Int {
        var result = func.hashCode()
        result = 31 * result + operands.hashCode()
        return result
    }

    override fun dump(): String {
        val builder = StringBuilder()
        builder.append("call $tp ${func.name}(")
        operands.joinTo(builder) { "$it:${it.type()}"}
        builder.append(")")
        return builder.toString()
    }

    override fun copy(newUsages: List<Value>): VoidCall {
        assert(newUsages.size == operands.size) {
            "should be"
        }

        return VoidCall(func, newUsages)
    }

    override fun type(): Type {
        return Type.Void
    }
}