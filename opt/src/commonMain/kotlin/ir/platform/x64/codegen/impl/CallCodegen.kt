package ir.platform.x64.codegen.impl

import asm.x64.Operand
import asm.x64.*
import ir.platform.x64.codegen.X64MacroAssembler
import ir.types.PrimitiveType
import ir.platform.x64.codegen.visitors.CallGPOperandValueVisitor
import ir.platform.x64.codegen.visitors.CallXmmOperandValueVisitor


internal class CallIntCodegen(val type: PrimitiveType, val asm: X64MacroAssembler) : CallGPOperandValueVisitor {
    private val size = type.sizeOf()

    operator fun invoke(dst: Operand, src: GPRegister) {
        CallGPOperandValueVisitor.apply(dst, src, this)
    }

    override fun rr(dst: GPRegister, src: GPRegister) {
        asm.copy(size, src, dst)
    }

    override fun ar(dst: Address, src: GPRegister) {
        asm.mov(size, src, dst)
    }

    override fun default(dst: Operand, src: Operand) {
        throw RuntimeException("Internal error: '${ir.instruction.Call.NAME}' dst=$dst, src=$src")
    }
}

class CallFloatCodegen(val type: PrimitiveType, val asm: Assembler) : CallXmmOperandValueVisitor {
    private val size = type.sizeOf()

    operator fun invoke(dst: Operand, src: XmmRegister) {
        CallXmmOperandValueVisitor.apply(dst, src, this)
    }

    override fun rr(dst: XmmRegister, src: XmmRegister) {
        if (dst == src) {
            return
        }
        asm.movf(size, src, dst)
    }

    override fun ar(dst: Address, src: XmmRegister) {
        asm.movf(size, src, dst)
    }

    override fun default(dst: Operand, src: Operand) {
        throw RuntimeException("Internal error: '${ir.instruction.Call.NAME}' dst=$dst, src=$src")
    }
}