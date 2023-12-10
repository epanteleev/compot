package ir.instruction

import ir.AnyFunctionPrototype
import ir.Value
import ir.instruction.utils.Visitor
import ir.types.Type

class Call private constructor(name: String, private val func: AnyFunctionPrototype, args: List<Value>):
    ValueInstruction(name, func.type(), args.toTypedArray()),
    Callable {
    init {
        require(func.type() != Type.Void) { "Must be non void" }
    }

    override fun arguments(): Array<Value> {
        return operands
    }

    override fun prototype(): AnyFunctionPrototype {
        return func
    }

    override fun copy(newUsages: List<Value>): Call {
        assert(newUsages.size == operands.size) {
            "should be, but newUsages=$newUsages"
        }

        return make(identifier, func, newUsages)
    }

    override fun visit(visitor: Visitor) {
        visitor.visit(this)
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
            require(Callable.isAppropriateTypes(func, args)) {
                args.joinToString(prefix = "inconsistent types in $name, prototype='${func.shortName()}', ")
                    { "$it: ${it.type()}" }
            }

            return registerUser(Call(name, func, args), args.iterator())
        }
    }
}