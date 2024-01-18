package ir.instruction

import ir.AnyFunctionPrototype
import ir.Value
import ir.instruction.utils.Visitor
import ir.types.Type

class VoidCall private constructor(private val func: AnyFunctionPrototype, args: List<Value>):
    Instruction(args.toTypedArray()),
    Callable {
    init {
        require(func.returnType() == Type.Void) { "Must be void" }
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
        builder.append("call ${type()} @${func.name}(")
        operands.joinTo(builder) { "$it:${it.type()}"}
        builder.append(")")
        return builder.toString()
    }

    override fun copy(newUsages: List<Value>): VoidCall {
        assert(newUsages.size == operands.size) {
            "should be, but newUsages=$newUsages"
        }

        return make(func, newUsages)
    }

    override fun visit(visitor: Visitor) {
        visitor.visit(this)
    }

    override fun type(): Type {
        return Type.Void
    }

    companion object {
        fun make(func: AnyFunctionPrototype, args: List<Value>): VoidCall {
            require(Callable.isAppropriateTypes(func, args)) {
                args.joinToString(prefix = "inconsistent types, prototype='${func.shortName()}', ")
                { "$it: ${it.type()}" }
            }

            return registerUser(VoidCall(func, args), args.iterator())
        }
    }
}