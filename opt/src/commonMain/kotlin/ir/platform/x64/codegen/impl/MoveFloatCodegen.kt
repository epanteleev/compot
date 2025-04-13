package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.types.*
import ir.instruction.Store
import ir.platform.x64.CallConvention.xmmTemp1
import ir.platform.x64.codegen.visitors.XmmOperandsVisitorUnaryOp


internal class MoveFloatCodegen(type: FloatingPointType, val asm: Assembler): XmmOperandsVisitorUnaryOp {
    private val size = type.sizeOf()

    operator fun invoke(dst: Operand, value: Operand) {
        XmmOperandsVisitorUnaryOp.apply(dst, value, this)
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

    override fun ra(dst: XmmRegister, src: Address) = default(dst, src)
    override fun rr(dst: XmmRegister, src: XmmRegister) = default(dst, src)
    override fun default(dst: Operand, src: Operand) {
        throw RuntimeException("Internal error: '${Store.NAME}' dst=$dst, src=$src")
    }
}