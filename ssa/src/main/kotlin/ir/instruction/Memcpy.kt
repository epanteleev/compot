package ir.instruction

import ir.Value
import ir.UnsignedIntegerConstant
import ir.types.Type
import ir.types.PointerType
import ir.instruction.utils.IRInstructionVisitor


class Memcpy private constructor(dst: Value, src: Value, private val length: UnsignedIntegerConstant): Instruction(arrayOf(dst, src)) {
    override fun <T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Memcpy

        return length == other.length && operands.contentEquals(other.operands())
    }

    override fun hashCode(): Int {
        var result = length.hashCode()
        result = 31 * result + operands[0].hashCode() + operands[1].hashCode()
        return result
    }

    override fun dump(): String {
        return "$NAME ${destination().type()} ${destination()}, ${destination().type()} ${source()}, ${length.type()} $length"
    }

    fun destination(): Value {
        assert(operands.size == 2) {
            "size should be 2 in $this instruction"
        }

        return operands[0]
    }

    fun source(): Value {
        assert(operands.size == 2) {
            "size should be 2 in $this instruction"
        }

        return operands[1]
    }

    fun length(): UnsignedIntegerConstant = length

    companion object {
        const val NAME = "memcpy"

        fun make(dst: Value, src: Value, length: UnsignedIntegerConstant): Memcpy {
            require(isAppropriateTypes(dst.type(), src.type())) {
                "inconsistent types: dst=$dst, src=$src"
            }

            return registerUser(Memcpy(dst, src, length), dst, src)
        }

        private fun isAppropriateTypes(dstType: Type, srcType: Type): Boolean {
            return dstType == srcType && dstType is PointerType
        }

        fun typeCheck(memcpy: Memcpy): Boolean {
            return isAppropriateTypes(memcpy.destination().type(), memcpy.source().type())
        }
    }
}