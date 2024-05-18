package ir.instruction

import ir.Value
import ir.UnsignedIntegerConstant
import ir.types.Type
import ir.types.PointerType
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class Memcpy private constructor(owner: Block, dst: Value, src: Value, length: UnsignedIntegerConstant):
    Instruction(owner, arrayOf(dst, src, length)) {
    override fun <T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Memcpy

        return operands.contentEquals(other.operands())
    }

    override fun hashCode(): Int {
        return operands[0].hashCode() + operands[1].hashCode() + operands[2].hashCode()
    }

    override fun dump(): String {
        return "$NAME ${destination().type()} ${destination()}, ${destination().type()} ${source()}, ${length().type()} ${length()}"
    }

    fun destination(): Value {
        assert(operands.size == 3) {
            "size should be 2 in $this instruction"
        }

        return operands[0]
    }

    fun source(): Value {
        assert(operands.size == 3) {
            "size should be 2 in $this instruction"
        }

        return operands[1]
    }

    fun length(): UnsignedIntegerConstant {
        assert(operands.size == 3) {
            "size should be 2 in $this instruction"
        }

        return operands[2] as UnsignedIntegerConstant
    }

    companion object {
        const val NAME = "memcpy"

        fun make(owner: Block, dst: Value, src: Value, length: UnsignedIntegerConstant): Memcpy {
            require(isAppropriateTypes(dst.type(), src.type())) {
                "inconsistent types: dst=$dst, src=$src"
            }

            return registerUser(Memcpy(owner, dst, src, length), dst, src)
        }

        private fun isAppropriateTypes(dstType: Type, srcType: Type): Boolean {
            return dstType == srcType && dstType is PointerType
        }

        fun typeCheck(memcpy: Memcpy): Boolean {
            return isAppropriateTypes(memcpy.destination().type(), memcpy.source().type())
        }
    }
}