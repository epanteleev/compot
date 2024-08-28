package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.types.IntegerType
import ir.instruction.SignExtend
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.codegen.visitors.GPOperandsVisitorUnaryOp


data class SignExtendCodegen(val fromType: IntegerType, val toType: IntegerType, val asm: Assembler):
    GPOperandsVisitorUnaryOp {
    private val toSize   = toType.sizeOf()
    private val fromSize = fromType.sizeOf()

    operator fun invoke(dst: Operand, src: Operand) {
        GPOperandsVisitorUnaryOp.apply(dst, src, this)
    }

    override fun rr(dst: GPRegister, src: GPRegister) {
        asm.movsext(fromSize, toSize, src, dst)
    }

    override fun ra(dst: GPRegister, src: Address) {
        asm.movsext(fromSize, toSize, src, dst)
    }

    override fun ar(dst: Address, src: GPRegister) {
        asm.movsext(fromSize, toSize, src, temp1)
        asm.mov(toSize, temp1, dst)
    }

    override fun aa(dst: Address, src: Address) {
        asm.movsext(fromSize, toSize, src, temp1)
        asm.mov(toSize, temp1, dst)
    }

    override fun ri(dst: GPRegister, src: Imm32) {
        asm.mov(toSize, src, dst)
    }

    override fun ai(dst: Address, src: Imm32) {
        asm.mov(toSize, src, dst)
    }

    override fun default(dst: Operand, src: Operand) {
        throw RuntimeException("Internal error: '${SignExtend.NAME}' dst=$dst, src=$$src")
    }
}