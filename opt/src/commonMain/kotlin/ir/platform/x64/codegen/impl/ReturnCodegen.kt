package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.instruction.Return
import ir.platform.x64.codegen.MacroAssembler
import ir.types.PrimitiveType
import ir.platform.x64.codegen.visitors.ReturnGPOperandVisitor
import ir.platform.x64.codegen.visitors.ReturnXmmOperandVisitor


class ReturnIntCodegen(val type: PrimitiveType, val asm: MacroAssembler) : ReturnGPOperandVisitor {
    private val size = type.sizeOf()

    operator fun invoke(dst: GPRegister, src: Operand) {
        ReturnGPOperandVisitor.apply(dst, src, this)
    }

    override fun rr(dst: GPRegister, src: GPRegister) {
        asm.copy(size, src, dst)
    }

    override fun ra(dst: GPRegister, src: Address) {
        asm.mov(size, src, dst)
    }

    override fun ri(dst: GPRegister, src: ImmInt) {
        asm.mov(size, src, dst)
    }

    override fun default(dst: Operand, src: Operand) {
        throw RuntimeException("Internal error: '${Return.NAME}' dst=$dst, src=$src")
    }
}

class ReturnFloatCodegen(val type: PrimitiveType, val asm: Assembler) : ReturnXmmOperandVisitor {
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