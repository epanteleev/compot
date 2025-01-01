package ir.instruction

import common.assertion
import ir.value.Value
import ir.value.constant.UnsignedIntegerConstant
import ir.types.Type
import ir.types.PtrType
import ir.instruction.utils.IRInstructionVisitor
import ir.module.block.Block


class Memcpy private constructor(id: Identity, owner: Block, dst: Value, src: Value, length: UnsignedIntegerConstant):
    Instruction(id, owner, arrayOf(dst, src, length)) {
    override fun <T> visit(visitor: IRInstructionVisitor<T>): T {
        return visitor.visit(this)
    }

    override fun dump(): String {
        return "$NAME ${destination().type()} ${destination()}, ${destination().type()} ${source()}, ${length().type()} ${length()}"
    }

    fun destination(): Value {
        assertion(operands.size == 3) {
            "size should be 2 in $this instruction"
        }

        return operands[DESTINATION]
    }

    fun source(): Value {
        assertion(operands.size == 3) {
            "size should be 2 in $this instruction"
        }

        return operands[SOURCE]
    }

    fun length(): UnsignedIntegerConstant {
        assertion(operands.size == 3) {
            "size should be 2 in $this instruction"
        }

        return operands[LENGTH] as UnsignedIntegerConstant
    }

    companion object {
        const val NAME = "memcpy"
        const val DESTINATION = 0
        const val SOURCE = 1
        const val LENGTH = 2

        fun memcpy(dst: Value, src: Value, length: UnsignedIntegerConstant): InstBuilder<Memcpy> = {
            id: Identity, owner: Block -> make(id, owner, dst, src, length)
        }

        private fun make(id: Identity, owner: Block, dst: Value, src: Value, length: UnsignedIntegerConstant): Memcpy {
            require(isAppropriateTypes(dst.type(), src.type(), length)) {
                "inconsistent types: dst=$dst:${dst.type()}, src=$src:${src.type()}"
            }

            return registerUser(Memcpy(id, owner, dst, src, length), dst, src)
        }

        private fun isAppropriateTypes(dstType: Type, srcType: Type, length: UnsignedIntegerConstant): Boolean {
            return true
            if (length.value() == 0UL) {
                return false
            }
            return dstType == srcType && dstType is PtrType
        }

        fun typeCheck(memcpy: Memcpy): Boolean {
            return isAppropriateTypes(memcpy.destination().type(), memcpy.source().type(), memcpy.length())
        }
    }
}