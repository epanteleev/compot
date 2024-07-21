package ir.instruction

import ir.types.*
import ir.value.Value
import common.assertion
import ir.value.TupleValue
import ir.module.block.Block
import ir.module.AnyFunctionPrototype
import ir.instruction.utils.IRInstructionVisitor


class TupleCall private constructor(id: Identity, owner: Block, private val func: AnyFunctionPrototype, args: Array<Value>, target: Block):
    TerminateTupleInstruction(id, owner, func.returnType() as TupleType, args, arrayOf(target)), TupleValue,
    Callable {

    override fun arguments(): Array<Value> {
        return operands
    }

    override fun prototype(): AnyFunctionPrototype {
        return func
    }

    override fun type(): TupleType {
        return func.returnType() as TupleType
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
        const val NAME = "call"

        fun make(id: Identity, owner: Block, func: AnyFunctionPrototype, args: List<Value>, target: Block): TupleCall {
            assertion(func.returnType() is TupleType) { "Must be non ${Type.Void}" }

            val argsArray = args.toTypedArray()
            require(Callable.isAppropriateTypes(func, argsArray)) {
                args.joinToString(prefix = "inconsistent types in '$id', prototype='${func.shortName()}', ")
                { "$it: ${it.type()}" }
            }

            return registerUser(TupleCall(id, owner, func, argsArray, target), args.iterator())
        }
    }
}