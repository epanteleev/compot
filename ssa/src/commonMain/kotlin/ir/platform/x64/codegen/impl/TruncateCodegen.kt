package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.types.IntegerType
import ir.instruction.Truncate
import ir.platform.x64.codegen.visitors.GPOperandsVisitorUnaryOp


data class TruncateCodegen(val fromType: IntegerType, val toType: IntegerType, val asm: Assembler):
    GPOperandsVisitorUnaryOp {
    private val toSize = toType.sizeOf()
    private val fromSize = fromType.sizeOf()

    operator fun invoke(dst: Operand, src: Operand) {
        GPOperandsVisitorUnaryOp.apply(dst, src, this)
    }

    override fun rr(dst: GPRegister, src: GPRegister) {
        if (dst == src) {
            return
        }
        //TODO not correct
        asm.mov(fromSize, src, dst)
    }

    override fun ra(dst: GPRegister, src: Address) {
        asm.mov(fromSize, src, dst)
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
        throw RuntimeException("Internal error: '${Truncate.NAME}' dst=$dst, src=$$src")
    }
}