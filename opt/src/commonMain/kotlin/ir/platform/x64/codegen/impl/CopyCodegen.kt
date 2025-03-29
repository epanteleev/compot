package ir.platform.x64.codegen.impl

import asm.Operand
import asm.x64.*
import ir.types.*
import ir.instruction.Copy
import ir.platform.x64.codegen.visitors.*
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.CallConvention.xmmTemp1
import ir.platform.x64.codegen.X64MacroAssembler


internal class CopyCodegen(val type: PrimitiveType, val asm: X64MacroAssembler): GPOperandsVisitorUnaryOp, XmmOperandsVisitorUnaryOp {
    private val size = type.sizeOf()

    operator fun invoke(dst: Operand, src: Operand) = when (type) {
        is FloatingPointType       -> XmmOperandsVisitorUnaryOp.apply(dst, src, this)
        is IntegerType, is PtrType -> GPOperandsVisitorUnaryOp.apply(dst, src, this)
        is UndefType -> {}
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

    override fun rrF(dst: XmmRegister, src: XmmRegister) {
        if (dst == src) {
            return
        }

        asm.movf(size, src, dst)
    }

    override fun raF(dst: XmmRegister, src: Address) {
        asm.movf(size, src, dst)
    }

    override fun arF(dst: Address, src: XmmRegister) {
        asm.movf(size, src, dst)
    }

    override fun aaF(dst: Address, src: Address) {
        if (dst == src) {
            return
        }

        asm.movf(size, src, xmmTemp1)
        asm.movf(size, xmmTemp1, dst)
    }

    override fun default(dst: Operand, src: Operand) {
        throw RuntimeException("Internal error: '${Copy.NAME}' dst=$dst, src=$src")
    }
}