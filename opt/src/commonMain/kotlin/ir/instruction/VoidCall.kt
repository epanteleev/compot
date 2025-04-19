package ir.instruction

import common.arrayWrapperOf
import common.assertion
import ir.attributes.FunctionAttribute
import ir.value.Value
import ir.module.block.Block
import ir.instruction.utils.IRInstructionVisitor
import ir.module.DirectFunctionPrototype
import ir.types.VoidType


class VoidCall private constructor(id: Identity,
                                   owner: Block,
                                   private val func: DirectFunctionPrototype,
                                   private val attributes: Set<FunctionAttribute>,
                                   args: Array<Value>,
                                   target: Block):
    TerminateInstruction(id, owner, args, arrayOf(target)), Callable {
    init {
        assertion(func.returnType() == VoidType) { "Must be $VoidType" }
    }

    override fun arguments(): List<Value> {
        return arrayWrapperOf(operands)
    }

    override fun arg(idx: Int, newValue: Value) {
        update(idx, newValue)
    }

    override fun prototype(): DirectFunctionPrototype {
        return func
    }

    override fun attributes(): Set<FunctionAttribute> = attributes

    override fun target(): Block {
        assertion(targets.size == 1) {
            "should be only one target, but '$targets' found"
        }

        return targets[0]
    }

    override fun dump(): String = buildString {
        append("call $VoidType @${func.name}")
        printArguments(this)
    }

    override fun<T> accept(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        fun call(func: DirectFunctionPrototype, args: List<Value>, attributes: Set<FunctionAttribute>, target: Block): InstBuilder<VoidCall> = {
            id: Identity, owner: Block -> make(id, owner, func, attributes, args, target)
        }

        private fun make(id: Identity, owner: Block, func: DirectFunctionPrototype, attributes: Set<FunctionAttribute>, args: List<Value>, target: Block): VoidCall {
            require(Callable.isAppropriateTypes(func, args)) {
                args.joinToString(prefix = "inconsistent types, prototype='${func.shortDescription()}', ")
                { "$it: ${it.type()}" }
            }
            val argsArray = args.toTypedArray()
            return registerUser(VoidCall(id, owner, func, attributes, argsArray, target), args.iterator())
        }
    }
}