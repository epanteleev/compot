package ir.platform.x64.codegen.impl

import asm.x64.Operand
import asm.x64.*
import ir.types.IntegerType
import ir.instruction.ZeroExtend
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.codegen.X64MacroAssembler
import ir.platform.x64.codegen.visitors.GPOperandsVisitorUnaryOp


internal class ZeroExtendCodegen(fromType: IntegerType, toType: IntegerType, val asm: X64MacroAssembler): GPOperandsVisitorUnaryOp {
    private val fromTypeSize = fromType.sizeOf()
    private val toTypeSize = toType.sizeOf()

    operator fun invoke(dst: Operand, src: Operand) {
        GPOperandsVisitorUnaryOp.apply(dst, src, this)
    }

    override fun rr(dst: GPRegister, src: GPRegister) {
        if (fromTypeSize == 4 && toTypeSize == 8) {
            asm.copy(toTypeSize, src, dst)
        } else {
            asm.movzext(fromTypeSize, toTypeSize, src, dst)
        }
    }

    override fun ra(dst: GPRegister, src: Address) {
        if (fromTypeSize == 4 && toTypeSize == 8) {
            asm.mov(fromTypeSize, src, dst)
        } else {
            asm.movzext(fromTypeSize, toTypeSize, src, dst)
        }
    }

    override fun ar(dst: Address, src: GPRegister) {
        if (fromTypeSize == 4 && toTypeSize == 8) {
            asm.mov(toTypeSize, src, dst)
        } else {
            asm.movzext(fromTypeSize, toTypeSize, src, temp1)
            asm.mov(toTypeSize, temp1, dst)
        }
    }

    override fun aa(dst: Address, src: Address) {
        if (fromTypeSize == 4 && toTypeSize == 8) {
            if (dst == src) {
                return
            }
            asm.mov(fromTypeSize, src, temp1)
            asm.mov(toTypeSize, temp1, dst)
        } else {
            asm.movzext(fromTypeSize, toTypeSize, src, temp1)
            asm.mov(toTypeSize, temp1, dst)
        }
    }

    override fun ri(dst: GPRegister, src: Imm) {
        val mask = (1L shl fromTypeSize * 8) - 1
        val value = src.value() and mask
        asm.copy(toTypeSize, Imm64.of(value), dst)
    }

    override fun ai(dst: Address, src: Imm) {
        val mask = (1L shl fromTypeSize * 8) - 1
        val value = src.value() and mask
        if (Imm.canBeImm32(value)) {
            asm.mov(toTypeSize, Imm32.of(value), dst)
        } else {
            asm.mov(fromTypeSize, src, temp1)
            asm.mov(toTypeSize, temp1, dst)
        }
    }

    override fun default(dst: Operand, src: Operand) {
        throw RuntimeException("Internal error: '${ZeroExtend.NAME}' dst=$dst, src=$$src")
    }
}