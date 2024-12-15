package ir.instruction

import common.arrayWrapperOf
import ir.types.*
import ir.value.Value
import common.assertion
import ir.attributes.FunctionAttribute
import ir.value.TupleValue
import ir.module.block.Block
import ir.instruction.utils.IRInstructionVisitor
import ir.module.DirectFunctionPrototype


class TupleCall private constructor(id: Identity,
                                    owner: Block,
                                    private val func: DirectFunctionPrototype,
                                    private val attributes: Set<FunctionAttribute>,
                                    args: Array<Value>,
                                    target: Block):
    TerminateTupleInstruction(id, owner, func.returnType() as TupleType, args, arrayOf(target)), TupleValue,
    Callable {

    override fun arguments(): List<Value> {
        return arrayWrapperOf(operands)
    }

    override fun prototype(): DirectFunctionPrototype {
        return func
    }

    override fun attributes(): Set<FunctionAttribute> = attributes

    override fun type(): TupleType {
        return func.returnType() as TupleType
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

    companion object {
        const val NAME = "call"

        fun call(func: DirectFunctionPrototype, args: List<Value>, attributes: Set<FunctionAttribute>, target: Block): InstBuilder<TupleCall> = {
                id: Identity, owner: Block -> make(id, owner, func, attributes, args, target)
        }

        private fun make(id: Identity, owner: Block, func: DirectFunctionPrototype, attributes: Set<FunctionAttribute>, args: List<Value>, target: Block): TupleCall {
            assertion(func.returnType() is TupleType) { "Must be non $VoidType" }


            require(Callable.isAppropriateTypes(func, args)) {
                args.joinToString(prefix = "inconsistent types in '$id', prototype='${func.shortDescription()}', ")
                { "$it: ${it.type()}" }
            }
            val argsArray = args.toTypedArray()
            return registerUser(TupleCall(id, owner, func, attributes, argsArray, target), args.iterator())
        }
    }
}