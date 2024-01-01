package ir.platform.x64.codegen

import asm.x64.*
import ir.types.*
import ir.instruction.Not
import ir.platform.x64.utils.ApplyClosure
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.utils.GPOperandVisitorUnaryOp


data class NotCodegen(val type: IntegerType, val asm: Assembler): GPOperandVisitorUnaryOp {
    private val size = type.size()

    operator fun invoke(dst: Operand, src: Operand) {
        ApplyClosure(dst, src, this as GPOperandVisitorUnaryOp)
    }

    override fun rr(dst: GPRegister, src: GPRegister) {
        if (dst == src) {
            asm.not(size, dst)
        } else {
            asm.mov(size, src, dst)
            asm.not(size, dst)
        }
    }

    override fun ra(dst: GPRegister, src: Address) {
        asm.mov(size, src, dst)
        asm.not(size, dst)
    }

    override fun ar(dst: Address, src: GPRegister) {
        asm.mov(size, src, dst)
        asm.not(size, dst)
    }

    override fun aa(dst: Address, src: Address) {
        if (dst == src) {
            asm.not(size, dst)
        } else {
            asm.mov(size, src, temp1)
            asm.not(size, temp1)
            asm.mov(size, temp1, dst)
        }
    }

    override fun ri(dst: GPRegister, src: Imm32) {
        asm.mov(size, Imm32(src.value().inv()), dst)
    }

    override fun ai(dst: Address, src: Imm32) {
        asm.mov(size, Imm32(src.value().inv()), dst)
    }

    override fun default(dst: Operand, src: Operand) {
        throw RuntimeException("Internal error: '${Not.NAME}' dst=$dst, src=$$src")
    }
}