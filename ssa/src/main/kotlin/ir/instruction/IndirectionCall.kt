package ir.instruction

import ir.Value
import ir.types.Type
import ir.AnyFunctionPrototype
import ir.IndirectFunctionPrototype
import ir.instruction.utils.Visitor


class IndirectionCall private constructor(name: String, pointer: Value, private val func: IndirectFunctionPrototype, args: List<Value>):
    ValueInstruction(name, func.returnType(), (args + pointer).toTypedArray()),
    Callable {
    init {
        assert(func.returnType() != Type.Void) { "Must be non ${Type.Void}" }
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

    override fun copy(newUsages: List<Value>): IndirectionCall {
        assert(newUsages.size == operands.size) {
            "should be, but newUsages=$newUsages"
        }

        return make(identifier, newUsages[0], func, newUsages.takeLast(newUsages.size - 2)) // TODO Creation new list
    }

    override fun visit(visitor: Visitor) {
        visitor.visit(this)
    }

    override fun dump(): String {
        val builder = StringBuilder()
        builder.append("%$identifier = call $tp ${pointer()}(")
        operands.joinTo(builder) { "$it:${it.type()}"}
        builder.append(")")
        return builder.toString()
    }

    companion object {
        fun make(name: String, pointer: Value, func: IndirectFunctionPrototype, args: List<Value>): IndirectionCall {
            require(Callable.isAppropriateTypes(func, args.toTypedArray())) {
                args.joinToString(prefix = "inconsistent types in $name, prototype='${func.shortName()}', ")
                { "$it: ${it.type()}" }
            }

            return registerUser(IndirectionCall(name, pointer, func, args), args.iterator())
        }
    }
}