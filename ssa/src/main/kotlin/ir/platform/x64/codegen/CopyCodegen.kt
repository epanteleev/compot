package ir.platform.x64.codegen

import asm.x64.*
import ir.types.*
import ir.instruction.Copy
import ir.platform.x64.utils.*
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.CallConvention.xmmTemp1


data class CopyCodegen(val type: PrimitiveType, val objFunc: ObjFunction): GPOperandVisitorUnaryOp, XmmOperandVisitorUnaryOp {
    val size = type.size()

    operator fun invoke(dst: AnyOperand, src: AnyOperand) {
        when (type) {
            is FloatingPointType            -> ApplyClosure(dst, src, this as XmmOperandVisitorUnaryOp)
            is IntegerType, is PointerType  -> ApplyClosure(dst, src, this as GPOperandVisitorUnaryOp)
            else -> throw RuntimeException("Unknown type=$type, dst=$dst, src=$src")
        }
    }

    override fun rr(dst: GPRegister, src: GPRegister) {
        if (dst == src) {
            return
        }

        objFunc.mov(size, src, dst)
    }

    override fun ra(dst: GPRegister, src: Address) {
        when (src) {
            is AddressLiteral -> objFunc.lea(size, src, dst)
            else              -> objFunc.mov(size, src, dst)
        }
    }

    override fun ar(dst: Address, src: GPRegister) {
        objFunc.mov(size, src, dst)
    }

    override fun aa(dst: Address, src: Address) {
        if (dst == src) {
            return
        }

        objFunc.mov(size, src, temp1)
        objFunc.mov(size, temp1, dst)
    }

    override fun ri(dst: GPRegister, src: Imm32) {
        objFunc.mov(size, src, dst)
    }

    override fun ai(dst: Address, src: Imm32) {
        objFunc.mov(size, src, dst)
    }

    override fun rrF(dst: XmmRegister, src: XmmRegister) {
        if (dst == src) {
            return
        }

        objFunc.movf(size, src, dst)
    }

    override fun raF(dst: XmmRegister, src: Address) {
        objFunc.movf(size, src, dst)
    }

    override fun arF(dst: Address, src: XmmRegister) {
        objFunc.movf(size, src, dst)
    }

    override fun aaF(dst: Address, src: Address) {
        if (dst == src) {
            return
        }

        objFunc.movf(size, src, xmmTemp1)
        objFunc.movf(size, xmmTemp1, dst)
    }

    override fun error(src: AnyOperand, dst: AnyOperand) {
        throw RuntimeException("Unimplemented: '${Copy.name}' dst=$dst, src=$src")
    }
}