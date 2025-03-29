package ir.platform.x64.codegen.impl

import asm.x64.*
import asm.Operand
import ir.instruction.Return
import ir.types.PrimitiveType
import ir.platform.x64.codegen.X64MacroAssembler
import ir.platform.x64.codegen.visitors.ReturnGPOperandVisitor


internal class ReturnIntCodegen(type: PrimitiveType, val asm: X64MacroAssembler) : ReturnGPOperandVisitor {
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
        asm.copy(size, src, dst)
    }

    override fun default(dst: Operand, src: Operand) {
        throw RuntimeException("Internal error: '${Return.NAME}' dst=$dst, src=$src")
    }
}