package ir.instruction

import common.*
import ir.value.Value
import ir.types.VoidType
import ir.module.block.Block
import ir.attributes.FunctionAttribute
import ir.module.IndirectFunctionPrototype
import ir.instruction.utils.IRInstructionVisitor


class IndirectionVoidCall private constructor(id: Identity,
                                              owner: Block,
                                              private val func: IndirectFunctionPrototype,
                                              private val attributes: Set<FunctionAttribute>,
                                              usages: Array<Value>, target: Block):
    TerminateInstruction(id, owner, usages, arrayOf(target)), IndirectionCallable {
    init {
        assertion(func.returnType() == VoidType) { "Must be $VoidType" }
    }

    override fun target(): Block {
        assertion(targets.size == 1) {
            "size should be 1 target in $this instruction, but '$targets' found"
        }

        return targets[0]
    }

    override fun pointer(): Value {
        assertion(operands.isNotEmpty()) {
            "size should be at least 1 operand in $this instruction"
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
        append("call $VoidType ${pointer()}(")
        arguments().joinTo(this) {
            "$it:${it.type()}"
        }
        append(") br label ")
        append(target())
    }

    companion object {
        fun call(pointer: Value, func: IndirectFunctionPrototype, args: List<Value>, attributes: Set<FunctionAttribute>, block: Block): InstBuilder<IndirectionVoidCall> = {
            id: Identity, owner: Block -> make(id, owner, pointer, func, attributes, args, block)
        }

        private fun make(id: Identity, owner: Block, pointer: Value, func: IndirectFunctionPrototype, attributes: Set<FunctionAttribute>, args: List<Value>, block: Block): IndirectionVoidCall {
            require(Callable.isAppropriateTypes(func, pointer, args)) {
                args.joinToString(prefix = "inconsistent types: pointer=${pointer}:${pointer.type()}, prototype='${func.shortDescription()}', ")
                { "$it: ${it.type()}" }
            }

            val operands = args.toTypedArray(pointer)
            return registerUser(IndirectionVoidCall(id, owner, func, attributes, operands, block), operands.iterator())
        }
    }
}