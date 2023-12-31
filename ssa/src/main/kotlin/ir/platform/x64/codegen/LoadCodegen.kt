package ir.platform.x64.codegen

import asm.x64.*
import ir.types.*
import ir.instruction.Load
import ir.platform.x64.utils.*
import ir.platform.x64.CallConvention.temp1


data class LoadCodegen(val type: PrimitiveType, val asm: Assembler): GPOperandVisitorUnaryOp, XmmOperandVisitorUnaryOp {
    private val size = type.size()

    operator fun invoke(dst: Operand, pointer: Operand) {
        when (type) {
            is FloatingPointType           -> ApplyClosure(dst, pointer, this as XmmOperandVisitorUnaryOp)
            is IntegerType, is PointerType -> ApplyClosure(dst, pointer, this as GPOperandVisitorUnaryOp)
            else -> throw RuntimeException("Unknown type=$type, dst=$dst, pointer=$pointer")
        }
    }

    override fun rr(dst: GPRegister, src: GPRegister) = default(dst, src)

    override fun ra(dst: GPRegister, src: Address) {
        asm.mov(size, src, dst)
    }

    override fun ar(dst: Address, src: GPRegister) = default(dst, src)

    override fun aa(dst: Address, src: Address) {
        asm.mov(size, src, temp1)
        asm.mov(size, temp1, dst)
    }

    override fun ri(dst: GPRegister, src: Imm32) {
        TODO("Not yet implemented")
    }

    override fun ai(dst: Address, src: Imm32) {
        TODO("Not yet implemented")
    }

    override fun rrF(dst: XmmRegister, src: XmmRegister) = default(dst, src)

    override fun raF(dst: XmmRegister, src: Address) {
        asm.movf(size, src, dst)
    }

    override fun arF(dst: Address, src: XmmRegister) = default(dst, src)

    override fun aaF(dst: Address, src: Address) {
        TODO("Not yet implemented")
    }

    override fun default(dst: Operand, src: Operand) {
        throw RuntimeException("Internal error: '${Load.NAME}' dst=$dst, pointer=$src")
    }
}