package ir.instruction

import ir.Value
import ir.types.Type
import ir.types.NonTrivialType
import ir.module.IndirectFunctionPrototype
import ir.instruction.utils.IRInstructionVisitor


class IndirectionCall private constructor(name: String, pointer: Value, private val func: IndirectFunctionPrototype, args: List<Value>):
    ValueInstruction(name, func.returnType() as NonTrivialType, (args + pointer).toTypedArray()),
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

    override fun prototype(): IndirectFunctionPrototype {
        return func
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
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
                args.joinToString(prefix = "inconsistent types in '$name', pointer=$pointer:${pointer.type()}, prototype='${func.shortName()}', ")
                { "$it: ${it.type()}" }
            }

            return registerUser(IndirectionCall(name, pointer, func, args), args.iterator())
        }
    }
}