package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.instruction.AnyPredicateType
import ir.instruction.Flag2Int
import ir.platform.x64.codegen.utils.ApplyClosure
import ir.platform.x64.codegen.utils.GPOperandsVisitorUnaryOp


class Flag2IntCodegen(private val asm: Assembler, private val size: Int): GPOperandsVisitorUnaryOp {

    operator fun invoke(dst: Operand, src: Operand) {
        ApplyClosure(dst, src, this as GPOperandsVisitorUnaryOp)
    }

    override fun rr(dst: GPRegister, src: GPRegister) {
        if (dst == src) {
            return
        }
        asm.mov(size, dst, src)
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

    override fun ri(dst: GPRegister, src: Imm32) {
        TODO("Not yet implemented")
    }

    override fun ai(dst: Address, src: Imm32) {
        TODO("Not yet implemented")
    }

    override fun default(dst: Operand, src: Operand) {
        throw RuntimeException("Internal error: '${Flag2Int.NAME}' dst=$dst, src=$src")
    }
}