package ir.platform.x64.codegen.impl

import asm.x64.Operand
import asm.x64.*
import ir.instruction.Return
import ir.types.PrimitiveType
import ir.platform.x64.codegen.visitors.ReturnXmmOperandVisitor


internal class ReturnFloatCodegen(val type: PrimitiveType, val asm: Assembler) : ReturnXmmOperandVisitor {
    private val size = type.sizeOf()

    operator fun invoke(dst: XmmRegister, src: Operand) {
        ReturnXmmOperandVisitor.apply(dst, src, this)
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

    override fun default(dst: Operand, src: Operand) {
        throw RuntimeException("Internal error: '${Return.NAME}' dst=$dst, src=$src")
    }
}