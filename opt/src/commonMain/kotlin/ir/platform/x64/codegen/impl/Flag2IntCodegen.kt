package ir.platform.x64.codegen.impl

import asm.Operand
import asm.x64.*
import ir.types.Type
import ir.instruction.Flag2Int
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.codegen.visitors.GPOperandsVisitorUnaryOp


class Flag2IntCodegen(private val toSize: Int, private val asm: Assembler): GPOperandsVisitorUnaryOp {
    operator fun invoke(dst: Operand, src: Operand) {
        if (toSize == fromSize && dst == src) {
            return
        }
        GPOperandsVisitorUnaryOp.apply(dst, src, this)
    }

    override fun rr(dst: GPRegister, src: GPRegister) {
        asm.movzext(fromSize, toSize, src, dst)
    }

    override fun ra(dst: GPRegister, src: Address) {
        TODO("Not yet implemented")
    }

    override fun ar(dst: Address, src: GPRegister) {
        TODO("Not yet implemented")
    }

    override fun aa(dst: Address, src: Address) {
        asm.movzext(fromSize, toSize, src, temp1)
        asm.mov(toSize, temp1, dst)
    }

    override fun ri(dst: GPRegister, src: Imm32) {
        TODO("Not yet implemented")
    }

    override fun ai(dst: Address, src: Imm32) {
        TODO("Not yet implemented")
    }

    override fun default(dst: Operand, src: Operand) {
        throw RuntimeException("Internal error: '${Flag2Int.NAME}' dst=$dst, src=$$src")
    }

    companion object {
        private val fromSize = Type.U8.sizeOf()
    }
}