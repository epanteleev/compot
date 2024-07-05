package ir.instruction

import common.assertion
import ir.value.Value
import ir.types.Type
import ir.module.AnyFunctionPrototype
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block
import ir.types.NonTrivialType
import kotlin.jvm.JvmInline


class Call private constructor(id: Identity, owner: Block, private val func: AnyFunctionPrototype, args: Array<Value>, target: Block):
    TerminateValueInstruction(id, owner, func.returnType() as NonTrivialType, args, arrayOf(target)),
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
        builder.append("%${name()} = call $tp @${func.name}(")
        operands.joinTo(builder) { "$it:${it.type()}"}
        builder.append(") bt label %${target()}")
        return builder.toString()
    }

    override fun target(): Block {
        assertion(targets.size == 1) {
            "size should be 1 target in $this instruction, but '$targets' found"
        }

        return targets[0]
    }

    companion object {
        fun make(id: Identity, owner: Block, func: AnyFunctionPrototype, args: List<Value>, target: Block): Call {
            assertion(func.returnType() != Type.Void) { "Must be non ${Type.Void}" }

            val argsArray = args.toTypedArray()
            require(Callable.isAppropriateTypes(func, argsArray)) {
                args.joinToString(prefix = "inconsistent types in '$id', prototype='${func.shortName()}', ")
                    { "$it: ${it.type()}" }
            }

            return registerUser(Call(id, owner, func, argsArray, target), args.iterator())
        }
    }
}