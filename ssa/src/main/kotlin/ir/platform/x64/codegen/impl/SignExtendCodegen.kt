package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.types.IntegerType
import ir.instruction.SignExtend
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.codegen.utils.ApplyClosure
import ir.platform.x64.codegen.utils.GPOperandsVisitorUnaryOp


data class SignExtendCodegen(val fromType: IntegerType, val toType: IntegerType, val asm: Assembler):
    GPOperandsVisitorUnaryOp {
    private val toSize   = toType.size()
    private val fromSize = fromType.size()

    operator fun invoke(dst: Operand, src: Operand) {
        ApplyClosure(dst, src, this as GPOperandsVisitorUnaryOp)
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
        TODO("Not yet implemented")
    }

    override fun ai(dst: Address, src: Imm32) {
        TODO("Not yet implemented")
    }

    override fun default(dst: Operand, src: Operand) {
        throw RuntimeException("Internal error: '${SignExtend.NAME}' dst=$dst, src=$$src")
    }
}