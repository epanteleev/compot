package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.types.IntegerType
import ir.instruction.ZeroExtend
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.codegen.MacroAssembler
import ir.platform.x64.codegen.visitors.GPOperandsVisitorUnaryOp


class ZeroExtendCodegen(fromType: IntegerType, toType: IntegerType, val asm: MacroAssembler): GPOperandsVisitorUnaryOp {
    private val fromTypeSize = fromType.sizeOf()
    private val toTypeSize = toType.sizeOf()

    operator fun invoke(dst: Operand, src: Operand) {
        GPOperandsVisitorUnaryOp.apply(dst, src, this)
    }

    override fun rr(dst: GPRegister, src: GPRegister) {
        if (fromTypeSize == 4 && toTypeSize == 8) {
            if (dst == src) {
                return
            }
            asm.copy(fromTypeSize, src, dst)
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
            asm.mov(fromTypeSize, src, dst)
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

    override fun ri(dst: GPRegister, src: Imm32) {
        asm.mov(toTypeSize, src, dst)
    }

    override fun ai(dst: Address, src: Imm32) {
        asm.mov(toTypeSize, src, dst)
    }

    override fun default(dst: Operand, src: Operand) {
        throw RuntimeException("Internal error: '${ZeroExtend.NAME}' dst=$dst, src=$$src")
    }
}