package ir.instruction

import ir.Value
import ir.types.*
import ir.instruction.utils.Visitor


class Store private constructor(pointer: Value, value: Value, private val valueType: NonTrivialType):
    Instruction(arrayOf(pointer, value)) {
    override fun dump(): String {
        return "$NAME ptr ${pointer()}, ${value().type()} ${value()}"
    }

    fun pointer(): Value {
        assert(operands.size == 2) {
            "size should be 2 in $this instruction"
        }

        return operands[0]
    }

    fun value(): Value {
        assert(operands.size == 2) {
            "size should be 2 in $this instruction"
        }

        return operands[1]
    }

    fun valueType(): NonTrivialType = valueType

    override fun<T> visit(visitor: Visitor<T>): T {
        return visitor.visit(this)
    }

    override fun hashCode(): Int {
        return valueType.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Store
        return pointer() == other.pointer() && value() == other.value() // TODO Unsafe
    }

    companion object {
        const val NAME = "store"

        fun make(pointer: Value, value: Value): Store {
            val pointerType = pointer.type()
            val valueType   = value.type()
            require(isAppropriateTypes(pointerType, valueType)) {
                "inconsistent types: pointer.type=$pointerType, value.type=$valueType"
            }

            return registerUser(Store(pointer, value, valueType), pointer, value)
        }

        private fun isAppropriateTypes(pointerType: Type, valueType: Type): Boolean {
            if (valueType !is PrimitiveType) {
                return false
            }

            if (pointerType !is PointerType) {
                return false
            }

            return true
        }

        fun isCorrect(store: Store): Boolean {
            return isAppropriateTypes(store.pointer().type(), store.value().type())
        }
    }
}