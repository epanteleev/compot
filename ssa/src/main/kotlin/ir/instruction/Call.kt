package ir.instruction

import ir.Value
import ir.types.Type
import ir.AnyFunctionPrototype
import ir.instruction.utils.Visitor
import ir.types.NonTrivialType


class Call private constructor(name: String, private val func: AnyFunctionPrototype, args: Array<Value>):
    ValueInstruction(name, func.returnType() as NonTrivialType, args),
    Callable {

    override fun arguments(): Array<Value> {
        return operands
    }

    override fun prototype(): AnyFunctionPrototype {
        return func
    }

    override fun<T> visit(visitor: Visitor<T>): T {
        return visitor.visit(this)
    }

    override fun dump(): String {
        val builder = StringBuilder()
        builder.append("%$identifier = call $tp @${func.name}(")
        operands.joinTo(builder) { "$it:${it.type()}"}
        builder.append(")")
        return builder.toString()
    }

    companion object {
        fun make(name: String, func: AnyFunctionPrototype, args: List<Value>): Call {
            assert(func.returnType() != Type.Void) { "Must be non ${Type.Void}" }

            val argsArray = args.toTypedArray()
            require(Callable.isAppropriateTypes(func, argsArray)) {
                args.joinToString(prefix = "inconsistent types in $name, prototype='${func.shortName()}', ")
                    { "$it: ${it.type()}" }
            }

            return registerUser(Call(name, func, argsArray), args.iterator())
        }
    }
}