package ir.instruction

import ir.value.Value
import ir.types.*
import common.assertion
import ir.module.block.Block
import ir.instruction.utils.IRInstructionVisitor


class Flag2Int private constructor(id: Identity, owner: Block, toType: IntegerType, value: Value):
    ValueInstruction(id, owner, toType, arrayOf(value)) {
    override fun dump(): String {
        return "%${name()} = $NAME ${value().type()} ${value()} to ${type()}"
    }

    fun value(): Value {
        assertion(operands.size == 1) {
            "size should be 1 in $this instruction"
        }

        return operands[0]
    }

    override fun type(): IntegerType {
        return tp as IntegerType
    }

    override fun<T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    companion object {
        const val NAME = "flag2int"

        fun make(id: Identity, owner: Block, toType: IntegerType, value: Value): Flag2Int {
            require(isAppropriateType(value.type())) {
                "inconsistent types in '$id': type=${value.type()}"
            }

            return registerUser(Flag2Int(id, owner, toType, value), value)
        }

        fun isAppropriateType(valueType: Type): Boolean {
            return valueType is FlagType
        }

        fun typeCheck(bitcast: Flag2Int): Boolean {
            return isAppropriateType(bitcast.value().type())
        }
    }
}