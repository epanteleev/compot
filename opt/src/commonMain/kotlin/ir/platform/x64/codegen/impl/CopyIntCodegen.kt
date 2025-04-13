package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.instruction.Copy
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.codegen.X64MacroAssembler
import ir.platform.x64.codegen.visitors.GPOperandsVisitorUnaryOp
import ir.types.*


internal class CopyIntCodegen(val type: PrimitiveType, val asm: X64MacroAssembler): GPOperandsVisitorUnaryOp {
    private val size = type.sizeOf()

    operator fun invoke(dst: Operand, src: Operand) = when (type) {
        is IntegerType, is PtrType -> GPOperandsVisitorUnaryOp.apply(dst, src, this)
        else -> default(dst, src)
    }

    override fun rr(dst: GPRegister, src: GPRegister) {
        asm.copy(size, src, dst)
    }

    override fun ra(dst: GPRegister, src: Address) {
        asm.mov(size, src, dst)
    }

    override fun ar(dst: Address, src: GPRegister) {
        asm.mov(size, src, dst)
    }

    override fun aa(dst: Address, src: Address) {
        if (dst == src) {
            return
        }

        asm.mov(size, src, temp1)
        asm.mov(size, temp1, dst)
    }

    override fun ri(dst: GPRegister, src: Imm32) {
        asm.copy(size, src, dst)
    }

    override fun ai(dst: Address, src: Imm32) {
        asm.mov(size, src, dst)
    }

    override fun default(dst: Operand, src: Operand) {
        throw RuntimeException("Internal error: '${Copy.NAME}' dst=$dst, src=$src")
    }
}