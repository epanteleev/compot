package ir.instruction

import common.arrayWith
import common.arrayWrapperOf
import common.assertion
import common.toTypedArray
import ir.attributes.FunctionAttribute
import ir.value.Value
import ir.module.IndirectFunctionPrototype
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block
import ir.types.*


class IndirectionCall private constructor(id: Identity, owner: Block,
                                          private val func: IndirectFunctionPrototype,
                                          private val attributes: Set<FunctionAttribute>,
                                          operands: Array<Value>,
                                          target: Block):
    TerminateValueInstruction(id, owner, func.returnType(), operands, arrayOf(target)),
    IndirectionCallable {
    init {
        assertion(func.returnType() !is TrivialType) { "Must be non trivial type" }
    }

    override fun type(): PrimitiveType = tp as PrimitiveType

    override fun target(): Block {
        assertion(targets.size == 1) {
            "size should be 1 target in $this instruction, but '$targets' found"
        }

        return targets[0]
    }

    override fun pointer(): Value {
        assertion(operands.isNotEmpty()) {
            "size should be at least 1 operand in $this instruction, but '${operands.joinToString { it.toString() }}' found"
        }

        return operands.last()
    }

    override fun pointer(newValue: Value) {
        update(operands.size - 1, newValue)
    }

    override fun arguments(): List<Value> {
        return arrayWrapperOf(arrayWith(operands.size - 1) { operands[it] })
    }

    override fun arg(idx: Int, newValue: Value) {
        if (idx >= operands.size - 1 || idx < 0) {
            throw IndexOutOfBoundsException("index=$idx, operands=${operands.joinToString { it.toString() }}")
        }

        update(idx, newValue)
    }

    override fun prototype(): IndirectFunctionPrototype {
        return func
    }

    override fun attributes(): Set<FunctionAttribute> = attributes

    override fun<T> accept(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    override fun dump(): String = buildString {
        append("%${name()} = call $tp ${pointer()}(")
        printArguments(this)
    }

    companion object {
        fun call(pointer: Value, func: IndirectFunctionPrototype, args: List<Value>, attributes: Set<FunctionAttribute>, target: Block): InstBuilder<IndirectionCall> = {
            id: Identity, owner: Block -> make(id, owner, pointer, func, attributes, args, target)
        }

        private fun make(id: Identity, owner: Block, pointer: Value, func: IndirectFunctionPrototype, attributes: Set<FunctionAttribute>, args: List<Value>, target: Block): IndirectionCall {
            require(Callable.isAppropriateTypes(func, args)) {
                args.joinToString(prefix = "inconsistent types in '$id', pointer=$pointer:${pointer.type()}, prototype='${func.shortDescription()}', ")
                { "$it: ${it.type()}" }
            }

            val operands = args.toTypedArray(pointer)
            return registerUser(IndirectionCall(id, owner, func, attributes, operands, target), *operands)
        }
    }
}