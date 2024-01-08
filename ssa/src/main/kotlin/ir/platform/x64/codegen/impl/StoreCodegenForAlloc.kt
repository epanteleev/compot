package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.types.*
import ir.instruction.Store
import ir.platform.x64.codegen.utils.ApplyClosure
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.CallConvention.xmmTemp1
import ir.platform.x64.codegen.utils.GPOperandVisitorUnaryOp
import ir.platform.x64.codegen.utils.XmmOperandVisitorUnaryOp


data class StoreCodegenForAlloc(val type: PrimitiveType, val asm: Assembler): GPOperandVisitorUnaryOp,
    XmmOperandVisitorUnaryOp {
    private val size = type.size()

    operator fun invoke(value: Operand, pointer: Operand) {
        when (type) {
            is FloatingPointType           -> ApplyClosure(pointer, value, this as XmmOperandVisitorUnaryOp)
            is IntegerType, is PointerType -> ApplyClosure(pointer, value, this as GPOperandVisitorUnaryOp)
            else -> throw RuntimeException("Unknown type=$type, value=$value, pointer=$pointer")
        }
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
        asm.mov(size, src, Address.from(dst, 0))
    }

    override fun ai(dst: Address, src: Imm32) {
        asm.mov(size, src, dst)
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

    override fun rr(dst: GPRegister, src: GPRegister) = default(dst, src)
    override fun ra(dst: GPRegister, src: Address) = default(dst, src)
    override fun raF(dst: XmmRegister, src: Address) = default(dst, src)
    override fun rrF(dst: XmmRegister, src: XmmRegister) = default(dst, src)
    override fun default(dst: Operand, src: Operand) {
        throw RuntimeException("Internal error: '${Store.NAME}' dst=$dst, pointer=$src")
    }
}