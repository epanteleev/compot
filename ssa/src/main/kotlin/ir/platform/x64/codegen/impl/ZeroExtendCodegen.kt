package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.types.IntegerType
import ir.instruction.ZeroExtend
import ir.platform.x64.codegen.utils.ApplyClosure
import ir.platform.x64.codegen.utils.GPOperandsVisitorUnaryOp


data class ZeroExtendCodegen(val fromType: IntegerType, val asm: Assembler): GPOperandsVisitorUnaryOp {
    private val typeSize = fromType.size()

    operator fun invoke(dst: Operand, src: Operand) {
        ApplyClosure(dst, src, this as GPOperandsVisitorUnaryOp)
    }

    override fun rr(dst: GPRegister, src: GPRegister) {
        if (dst == src) {
            return
        }
        asm.mov(typeSize, src, dst)
    }

    override fun ra(dst: GPRegister, src: Address) {
        asm.mov(typeSize, src, dst)
    }

    override fun ar(dst: Address, src: GPRegister) {
        TODO("Not yet implemented")
    }

    override fun aa(dst: Address, src: Address) {
        TODO("Not yet implemented")
    }

    override fun ri(dst: GPRegister, src: Imm32) {
        TODO("Not yet implemented")
    }

    override fun ai(dst: Address, src: Imm32) {
        TODO("Not yet implemented")
    }

    override fun default(dst: Operand, src: Operand) {
        throw RuntimeException("Internal error: '${ZeroExtend.NAME}' dst=$dst, src=$$src")
    }
}