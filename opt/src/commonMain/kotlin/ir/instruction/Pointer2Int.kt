package ir.instruction

import ir.types.*
import ir.value.Value
import common.assertion
import ir.module.block.Block
import ir.instruction.utils.IRInstructionVisitor


class Pointer2Int private constructor(id: Identity, owner: Block, private val toType: IntegerType, value: Value):
    ValueInstruction(id, owner, arrayOf(value)) {
    override fun dump(): String {
        return "%${name()} = $NAME ${value().type()} ${value()} to ${type()}"
    }

    fun value(): Value {
        assertion(operands.size == 1) {
            "size should be 1 in $this instruction"
        }

        return operands[SOURCE]
    }

    override fun type(): IntegerType = toType

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "ptr2int"
        const val SOURCE = 0

        fun ptr2int(value: Value, toType: IntegerType): InstBuilder<Pointer2Int> = { id: Identity, owner: Block ->
            make(id, owner, toType, value)
        }

        private fun make(id: Identity, owner: Block, toType: IntegerType, value: Value): Pointer2Int {
            val valueType = value.type()
            require(isAppropriateType(valueType)) {
                "inconsistent types in '$id': ty=$toType, value=$value:$valueType"
            }

            return registerUser(Pointer2Int(id, owner, toType, value), value)
        }

        private fun isAppropriateType(valueType: Type): Boolean {
            return valueType is PtrType
        }

        fun typeCheck(ptr2int: Pointer2Int): Boolean {
            return isAppropriateType(ptr2int.value().type())
        }
    }
}