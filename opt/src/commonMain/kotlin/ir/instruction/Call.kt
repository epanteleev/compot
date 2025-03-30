package ir.instruction

import common.arrayWrapperOf
import common.assertion
import ir.attributes.FunctionAttribute
import ir.value.Value
import ir.instruction.utils.IRInstructionVisitor
import ir.module.DirectFunctionPrototype
import ir.module.block.Block
import ir.types.PrimitiveType
import ir.types.TupleType
import ir.types.VoidType


class Call private constructor(id: Identity, owner: Block, private val func: DirectFunctionPrototype, private val attributes: Set<FunctionAttribute>, args: Array<Value>, target: Block):
    TerminateValueInstruction(id, owner, func.returnType(), args, arrayOf(target)),
    Callable {

    init {
        assertion(func.returnType() != VoidType) { "Must be non $VoidType" }
    }

    override fun type(): PrimitiveType {
        return tp as PrimitiveType
    }

    override fun arguments(): List<Value> {
        return arrayWrapperOf(operands)
    }

    override fun arg(idx: Int, newValue: Value) = owner.df {
        update(idx, newValue)
    }

    override fun prototype(): DirectFunctionPrototype {
        return func
    }

    override fun<T> accept(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    override fun dump(): String = buildString {
        append("%${name()} = call $tp @${func.name}")
        printArguments(this)
    }

    override fun target(): Block {
        assertion(targets.size == 1) {
            "size should be 1 target in $this instruction, but '$targets' found"
        }

        return targets[0]
    }

    override fun attributes(): Set<FunctionAttribute> = attributes

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

        fun call(func: DirectFunctionPrototype, args: List<Value>, attributes: Set<FunctionAttribute>, target: Block): InstBuilder<Call> = {
            id: Identity, owner: Block -> make(id, owner, func, args, attributes, target)
        }

        private fun make(id: Identity, owner: Block, func: DirectFunctionPrototype, args: List<Value>, attributes: Set<FunctionAttribute>, target: Block): Call {
            assertion(func.returnType() != VoidType) { "Must be non $VoidType" }

            require(Callable.isAppropriateTypes(func, args)) {
                args.joinToString(prefix = "inconsistent types in '$id', prototype='${func.shortDescription()}', ")
                    { "$it: ${it.type()}" }
            }

            return registerUser(Call(id, owner, func, attributes, args.toTypedArray(), target), args.iterator())
        }
    }
}