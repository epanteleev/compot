package ir.platform.x64.codegen.impl

import asm.x64.Operand
import asm.x64.*
import ir.types.*
import ir.instruction.Copy
import ir.platform.x64.codegen.visitors.*
import ir.platform.x64.CallConvention.xmmTemp1
import ir.platform.x64.codegen.X64MacroAssembler


internal class CopyFloatCodegen(type: FloatingPointType, val asm: X64MacroAssembler): XmmOperandsVisitorUnaryOp {
    private val size = type.sizeOf()

    operator fun invoke(dst: Operand, src: Operand) {
        XmmOperandsVisitorUnaryOp.apply(dst, src, this)
    }

    override fun rr(dst: XmmRegister, src: XmmRegister) {
        if (dst == src) {
            return
        }

        asm.movf(size, src, dst)
    }

    override fun ra(dst: XmmRegister, src: Address) {
        asm.movf(size, src, dst)
    }

    override fun ar(dst: Address, src: XmmRegister) {
        asm.movf(size, src, dst)
    }

    override fun aa(dst: Address, src: Address) {
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