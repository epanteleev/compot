package ir.instruction

import common.arrayWith
import common.arrayWrapperOf
import common.assertion
import common.toTypedArray
import ir.value.Value
import ir.types.Type
import ir.module.IndirectFunctionPrototype
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class IndirectionCall private constructor(id: Identity, owner: Block,
                                          private val func: IndirectFunctionPrototype,
                                          operands: Array<Value>,
                                          target: Block):
    TerminateValueInstruction(id, owner, func.returnType(), operands, arrayOf(target)),
    Callable {
    init {
        assertion(func.returnType() != Type.Void) { "Must be non ${Type.Void}" }
    }

    override fun target(): Block {
        assertion(targets.size == 1) {
            "size should be 1 target in $this instruction, but '$targets' found"
        }

        return targets[0]
    }

    fun pointer(): Value {
        assertion(operands.isNotEmpty()) {
            "size should be at least 1 operand in $this instruction, but '${operands.joinToString { it.toString() }}' found"
        }

        return operands.last()
    }

    override fun arguments(): List<Value> {
        return arrayWrapperOf(arrayWith(operands.size - 1) { operands[it] })
    }

    override fun prototype(): IndirectFunctionPrototype {
        return func
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    override fun dump(): String {
        val builder = StringBuilder()
        builder.append("%${name()} = call $tp ${pointer()}(")
        printArguments(builder)
        return builder.toString()
    }

    companion object {
        fun make(id: Identity, owner: Block, pointer: Value, func: IndirectFunctionPrototype, args: List<Value>, target: Block): IndirectionCall {
            require(Callable.isAppropriateTypes(func, args)) {
                args.joinToString(prefix = "inconsistent types in '$id', pointer=$pointer:${pointer.type()}, prototype='${func.shortDescription()}', ")
                { "$it: ${it.type()}" }
            }

            val operands = args.toTypedArray(pointer)
            return registerUser(IndirectionCall(id, owner, func, operands, target), operands.iterator())
        }
    }
}