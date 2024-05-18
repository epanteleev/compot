package ir.instruction

import ir.Value
import ir.types.Type
import ir.module.AnyFunctionPrototype
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block
import ir.types.NonTrivialType


class Call private constructor(name: String, owner: Block, private val func: AnyFunctionPrototype, args: Array<Value>):
    ValueInstruction(name, owner, func.returnType() as NonTrivialType, args),
    Callable {

    override fun arguments(): Array<Value> {
        return operands
    }

    override fun prototype(): AnyFunctionPrototype {
        return func
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    override fun dump(): String {
        val builder = StringBuilder()
        builder.append("%$id = call $tp @${func.name}(")
        operands.joinTo(builder) { "$it:${it.type()}"}
        builder.append(")")
        return builder.toString()
    }

    companion object {
        fun make(name: String, owner: Block, func: AnyFunctionPrototype, args: List<Value>): Call {
            assert(func.returnType() != Type.Void) { "Must be non ${Type.Void}" }

            val argsArray = args.toTypedArray()
            require(Callable.isAppropriateTypes(func, argsArray)) {
                args.joinToString(prefix = "inconsistent types in $name, prototype='${func.shortName()}', ")
                    { "$it: ${it.type()}" }
            }

            return registerUser(Call(name, owner, func, argsArray), args.iterator())
        }
    }
}