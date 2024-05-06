package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.instruction.Memcpy
import ir.UnsignedIntegerConstant
import ir.platform.x64.codegen.utils.ApplyClosure
import ir.platform.x64.codegen.utils.GPOperandsVisitorUnaryOp


class MemcpyCodegen(val length: UnsignedIntegerConstant, val asm: Assembler):
    GPOperandsVisitorUnaryOp {
    operator fun invoke(dst: Operand, src: Operand) {
        ApplyClosure(dst, src, this)
    }

    override fun rr(dst: GPRegister, src: GPRegister) {
        TODO("Not yet implemented")
    }

    override fun ra(dst: GPRegister, src: Address) {
        TODO("Not yet implemented")
    }

    override fun ar(dst: Address, src: GPRegister) {
        TODO("Not yet implemented")
    }

    override fun aa(dst: Address, src: Address) {
        TODO("Not yet implemented")
    }

    override fun ri(dst: GPRegister, src: Imm32) = default(dst, src)

    override fun ai(dst: Address, src: Imm32) = default(dst, src)

    override fun default(dst: Operand, src: Operand) {
        throw RuntimeException("Internal error: '${Memcpy.NAME}' dst=$dst, src=$src")
    }
}