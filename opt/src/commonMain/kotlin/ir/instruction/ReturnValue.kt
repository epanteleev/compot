package ir.instruction

import ir.types.*
import ir.value.*
import common.arrayFrom
import ir.module.block.Block
import ir.instruction.utils.IRInstructionVisitor


class ReturnValue private constructor(id: Identity, owner: Block, val returnType: Type, values: Array<Value>): Return(id, owner, values) {
    override fun dump(): String {
        val stringBuilder = StringBuilder("$NAME ${type()}")
        for (value in operands) {
            stringBuilder.append(" ")
            stringBuilder.append(value)
        }
        return stringBuilder.toString()
    }

    fun returnValue(index: Int): Value {
        if (index >= operands.size || index < 0) {
            throw IndexOutOfBoundsException("index=$index, operands=${operands.joinToString { it.toString() }}")
        }

        return operands[index]
    }

    fun type(): Type = returnType

    override fun<T> accept(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "ret"
        const val RET_VALUE = 0

        fun ret(returnType: Type, values: Array<Value>): InstBuilder<Return> = { id: Identity, owner: Block ->
            make(id, owner, returnType, values)
        }

        fun make(id: Identity, owner: Block, returnType: Type, values: Array<Value>): Return {
            require(isAppropriateType(returnType, values)) {
                "cannot be $returnType, but values=${values.joinToString { it.toString() }}"
            }

            return registerUser(ReturnValue(id, owner, returnType, values), values.iterator())
        }

        private fun isAppropriateType(retType: Type, values: Array<Value>): Boolean {
            if (retType is VoidType) {
                return false
            }
            if (retType is FlagType) {
                return false
            }
            if (retType is UndefType) {
                return false
            }
            if (retType is TupleType) {
                val array = arrayFrom(values) { it.asType<PrimitiveType>() }
                return retType == TupleType(array)
            }
            return true
        }

        fun typeCheck(retValue: ReturnValue): Boolean {
            return isAppropriateType(retValue.type(), retValue.operands)
        }
    }
}