package ir.instruction

import ir.AnyFunctionPrototype
import ir.Value
import ir.instruction.utils.Visitor
import ir.types.Type


class VoidCall private constructor(private val func: AnyFunctionPrototype, args: Array<Value>):
    Instruction(args),
    Callable {
    init {
        assert(func.returnType() == Type.Void) { "Must be ${Type.Void}" }
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
        return func == other.func
    }

    override fun hashCode(): Int {
        return func.hashCode()
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

    override fun<T> visit(visitor: Visitor<T>): T {
        return visitor.visit(this)
    }

    override fun type(): Type {
        return Type.Void
    }

    companion object {
        fun make(func: AnyFunctionPrototype, args: List<Value>): VoidCall {
            val argsArray = args.toTypedArray()
            require(Callable.isAppropriateTypes(func, argsArray)) {
                args.joinToString(prefix = "inconsistent types, prototype='${func.shortName()}', ")
                { "$it: ${it.type()}" }
            }

            return registerUser(VoidCall(func, argsArray), args.iterator())
        }
    }
}