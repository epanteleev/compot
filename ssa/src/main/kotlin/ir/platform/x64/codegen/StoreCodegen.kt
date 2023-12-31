package ir.platform.x64.codegen

import asm.x64.*
import ir.types.*
import ir.instruction.Store
import ir.platform.x64.utils.ApplyClosure
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.CallConvention.xmmTemp1
import ir.platform.x64.utils.GPOperandVisitorUnaryOp
import ir.platform.x64.utils.XmmOperandVisitorUnaryOp


data class StoreCodegen(val type: PrimitiveType, val objFunc: Assembler): GPOperandVisitorUnaryOp, XmmOperandVisitorUnaryOp {
    private val size = type.size()

    operator fun invoke(value: Operand, pointer: Operand) {
        when (type) {
            is FloatingPointType           -> ApplyClosure(pointer, value, this as XmmOperandVisitorUnaryOp)
            is IntegerType, is PointerType -> ApplyClosure(pointer, value, this as GPOperandVisitorUnaryOp)
            else -> throw RuntimeException("Unknown type=$type, value=$value, pointer=$pointer")
        }
    }

    override fun rr(dst: GPRegister, src: GPRegister) {
        objFunc.mov(size, src, Address.from(dst, 0))
    }

    override fun ra(dst: GPRegister, src: Address) {
        TODO("Not yet implemented")
    }

    override fun ar(dst: Address, src: GPRegister) {
        objFunc.mov(size, src, dst)
    }

    override fun aa(dst: Address, src: Address) {
        objFunc.mov(size, src, temp1)
        objFunc.mov(size, temp1, dst)
    }

    override fun ri(dst: GPRegister, src: Imm32) {
        objFunc.mov(size, src, Address.from(dst, 0))
    }

    override fun ai(dst: Address, src: Imm32) {
        objFunc.mov(size, src, dst)
    }

    override fun rrF(dst: XmmRegister, src: XmmRegister) = default(dst, src)

    override fun raF(dst: XmmRegister, src: Address) {
        objFunc.movf(size, src, dst)
    }

    override fun arF(dst: Address, src: XmmRegister) {
        objFunc.movf(size, src, dst)
    }

    override fun aaF(dst: Address, src: Address) {
        objFunc.movf(size, src, xmmTemp1)
        objFunc.movf(size, xmmTemp1, dst)
    }

    override fun default(dst: Operand, src: Operand) {
        if (dst is GPRegister && src is XmmRegister) {
            objFunc.movf(size, src, Address.from(dst, 0))
        } else {
            throw RuntimeException("Internal error: '${Store.NAME}' dst=$dst, pointer=$src")
        }
    }
}