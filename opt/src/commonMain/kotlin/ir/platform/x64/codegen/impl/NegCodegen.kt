package ir.platform.x64.codegen.impl

import asm.x64.Operand
import asm.x64.*
import ir.types.*
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.codegen.X64MacroAssembler
import ir.platform.x64.codegen.visitors.GPOperandsVisitorUnaryOp


internal class NegCodegen(val type: PrimitiveType, val asm: X64MacroAssembler): GPOperandsVisitorUnaryOp {
    private val size: Int = type.sizeOf()

    operator fun invoke(dst: Operand, src: Operand) {
        GPOperandsVisitorUnaryOp.apply(dst, src, this)
    }

    override fun rr(dst: GPRegister, src: GPRegister) {
        asm.copy(size, src, dst)
        asm.neg(size, dst)
    }

    override fun ra(dst: GPRegister, src: Address) {
        asm.mov(size, src, dst)
        asm.neg(size, dst)
    }

    override fun ar(dst: Address, src: GPRegister) {
        asm.mov(size, src, dst)
        asm.neg(size, dst)
    }

    override fun aa(dst: Address, src: Address) {
        if (dst == src) {
            asm.neg(size, dst)
        } else {
            asm.mov(size, src, temp1)
            asm.neg(size, temp1)
            asm.mov(size, temp1, dst)
        }
    }

    override fun ri(dst: GPRegister, src: Imm) {
        asm.copy(size, Imm64.of(-src.value()), dst)
    }

    override fun ai(dst: Address, src: Imm) {
        if (Imm.canBeImm32(src.value())) {
            asm.mov(size, Imm32.of(-src.value()), dst)
        } else {
            asm.mov(size, Imm64.of(-src.value()), temp1)
            asm.mov(size, temp1, dst)
        }
    }

    override fun default(dst: Operand, src: Operand) {
        throw RuntimeException("Internal error: ${ir.instruction.Neg.NAME} dst=$dst, src=$src")
    }
}