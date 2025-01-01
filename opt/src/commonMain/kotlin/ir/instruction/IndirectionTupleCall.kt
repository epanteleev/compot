package ir.instruction

import common.*
import ir.types.*
import ir.value.Value
import ir.module.block.Block
import ir.attributes.FunctionAttribute
import ir.instruction.utils.IRInstructionVisitor
import ir.module.IndirectFunctionPrototype


class IndirectionTupleCall private constructor(
    id: Identity, owner: Block,
    private val func: IndirectFunctionPrototype,
    private val attributes: Set<FunctionAttribute>,
    operands: Array<Value>,
    target: Block
) :
    TerminateTupleInstruction(id, owner, func.returnType().asType(), operands, arrayOf(target)),
    Callable {

    override fun type(): TupleType = tp.asType()

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

    override fun attributes(): Set<FunctionAttribute> = attributes

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    override fun dump(): String {
        val builder = StringBuilder()
        builder.append("%${name()} = $NAME $tp ${pointer()}(")
        printArguments(builder)
        return builder.toString()
    }

    companion object {
        const val NAME = "icall"

        fun call(pointer: Value, func: IndirectFunctionPrototype, args: List<Value>, attributes: Set<FunctionAttribute>, target: Block): InstBuilder<IndirectionTupleCall> = {
                id: Identity, owner: Block -> make(id, owner, pointer, func, attributes, args, target)
        }

        private fun make(id: Identity, owner: Block, pointer: Value, func: IndirectFunctionPrototype, attributes: Set<FunctionAttribute>, args: List<Value>, target: Block): IndirectionTupleCall {
            require(Callable.isAppropriateTypes(func, args)) {
                args.joinToString(prefix = "inconsistent types in '$id', pointer=$pointer:${pointer.type()}, prototype='${func.shortDescription()}', ")
                { "$it: ${it.type()}" }
            }

            val operands = args.toTypedArray(pointer)
            return registerUser(IndirectionTupleCall(id, owner, func, attributes, operands, target), *operands)
        }
    }
}