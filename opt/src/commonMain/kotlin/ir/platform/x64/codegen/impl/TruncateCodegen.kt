package ir.platform.x64.codegen.impl

import asm.Operand
import asm.x64.*
import ir.types.IntegerType
import ir.instruction.Truncate
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.codegen.X64MacroAssembler
import ir.platform.x64.codegen.visitors.GPOperandsVisitorUnaryOp


data class TruncateCodegen(val fromType: IntegerType, val toType: IntegerType, val asm: X64MacroAssembler):
    GPOperandsVisitorUnaryOp {
    private val toSize = toType.sizeOf()
    private val fromSize = fromType.sizeOf()

    operator fun invoke(dst: Operand, src: Operand) {
        GPOperandsVisitorUnaryOp.apply(dst, src, this)
    }

    override fun rr(dst: GPRegister, src: GPRegister) {
        asm.copy(toSize, src, dst)
    }

    override fun ra(dst: GPRegister, src: Address) {
        asm.mov(toSize, src, dst)
    }

    override fun ar(dst: Address, src: GPRegister) {
        asm.mov(toSize, src, dst)
    }

    override fun aa(dst: Address, src: Address) {
        asm.mov(fromSize, src, temp1)
        asm.mov(toSize, temp1, dst)
    }

    override fun ri(dst: GPRegister, src: Imm32) {
        asm.mov(toSize, src, dst)
    }

    override fun ai(dst: Address, src: Imm32) {
        asm.mov(toSize, src, dst)
    }

    override fun default(dst: Operand, src: Operand) {
        throw RuntimeException("Internal error: '${Truncate.NAME}' dst=$dst, src=$$src")
    }
}