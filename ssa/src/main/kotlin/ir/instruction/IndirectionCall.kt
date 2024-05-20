package ir.instruction

import ir.Value
import ir.types.Type
import ir.types.NonTrivialType
import ir.module.IndirectFunctionPrototype
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class IndirectionCall private constructor(id: Identity, owner: Block,
                                          pointer: Value,
                                          private val func: IndirectFunctionPrototype,
                                          args: List<Value>,
                                          target: Block):
    TerminateValueInstruction(id, owner, func.returnType() as NonTrivialType, (args + pointer).toTypedArray(), arrayOf(target)),
    Callable {
    init {
        assert(func.returnType() != Type.Void) { "Must be non ${Type.Void}" }
    }

    override fun target(): Block {
        assert(targets.size == 1) {
            "size should be 1 target in $this instruction, but '$targets' found"
        }

        return targets[0]
    }

    fun pointer(): Value {
        assert(operands.isNotEmpty()) {
            "size should be at least 1 operand in $this instruction, but '${operands.joinToString { it.toString() }}' found"
        }

        return operands[0]
    }

    override fun arguments(): Array<Value> {
        return operands
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
        operands.joinTo(builder) { "$it:${it.type()}"}
        builder.append(")")
        return builder.toString()
    }

    companion object {
        fun make(id: Identity, owner: Block, pointer: Value, func: IndirectFunctionPrototype, args: List<Value>, target: Block): IndirectionCall {
            require(Callable.isAppropriateTypes(func, args.toTypedArray())) {
                args.joinToString(prefix = "inconsistent types in '$id', pointer=$pointer:${pointer.type()}, prototype='${func.shortName()}', ")
                { "$it: ${it.type()}" }
            }

            return registerUser(IndirectionCall(id, owner, pointer, func, args, target), args.iterator())
        }
    }
}