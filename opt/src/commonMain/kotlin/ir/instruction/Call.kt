package ir.instruction

import common.arrayWrapperOf
import common.assertion
import common.forEachWith
import ir.value.Value
import ir.types.Type
import ir.module.AnyFunctionPrototype
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block
import ir.types.NonTrivialType
import ir.types.PrimitiveType
import ir.types.TupleType
import kotlin.jvm.JvmInline


class Call private constructor(id: Identity, owner: Block, private val func: AnyFunctionPrototype, args: Array<Value>, target: Block):
    TerminateValueInstruction(id, owner, func.returnType(), args, arrayOf(target)),
    Callable {

    init {
        assertion(func.returnType() != Type.Void) { "Must be non ${Type.Void}" }
    }

    override fun type(): PrimitiveType {
        return tp as PrimitiveType
    }

    override fun arguments(): List<Value> {
        return arrayWrapperOf(operands)
    }

    override fun prototype(): AnyFunctionPrototype {
        return func
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    override fun dump(): String {
        val builder = StringBuilder()
        builder.append("%${name()} = call $tp @${func.name}")
        printArguments(builder)
        return builder.toString()
    }

    override fun target(): Block {
        assertion(targets.size == 1) {
            "size should be 1 target in $this instruction, but '$targets' found"
        }

        return targets[0]
    }

    fun proj(index: Int): Projection? {
        if (tp !is TupleType) {
            throw IllegalStateException("type must be TupleType, but $tp found")
        }
        for (user in usedIn()) {
            user as Projection
            if (user.index() == index) {
                return user
            }
        }
        return null
    }

    companion object {
        const val NAME = "call"

        fun make(id: Identity, owner: Block, func: AnyFunctionPrototype, args: List<Value>, target: Block): Call {
            assertion(func.returnType() != Type.Void) { "Must be non ${Type.Void}" }


            require(Callable.isAppropriateTypes(func, args)) {
                args.joinToString(prefix = "inconsistent types in '$id', prototype='${func.shortName()}', ")
                    { "$it: ${it.type()}" }
            }

            return registerUser(Call(id, owner, func, args.toTypedArray(), target), args.iterator())
        }
    }
}