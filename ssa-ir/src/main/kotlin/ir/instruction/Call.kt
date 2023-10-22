package ir.instruction

import ir.*
import ir.types.Type

class Call(name: String, tp: Type, private val func: AnyFunctionPrototype, args: List<Value>):
    ValueInstruction(name, tp, args.toTypedArray()),
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
            "should be"
        }

        return Call(identifier, tp, func, newUsages)
    }

    override fun dump(): String {
        val builder = StringBuilder()
        builder.append("%$identifier = call $tp ${func.name}(")
        operands.joinTo(builder) { "$it:${it.type()}"}
        builder.append(")")
        return builder.toString()
    }
}