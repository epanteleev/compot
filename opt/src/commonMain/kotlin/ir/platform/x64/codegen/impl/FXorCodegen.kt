package ir.platform.x64.codegen.impl

import asm.x64.Operand
import asm.x64.*
import ir.platform.x64.CallConvention.fpRet
import ir.types.*
import ir.platform.x64.CallConvention.xmmTemp1
import ir.platform.x64.codegen.X64MacroAssembler
import ir.platform.x64.codegen.visitors.XmmOperandsVisitorBinaryOp


internal class FXorCodegen(val type: FloatingPointType, val asm: X64MacroAssembler): XmmOperandsVisitorBinaryOp {
    private val size: Int = type.sizeOf()

    operator fun invoke(dst: Operand, first: Operand, second: Operand) {
        XmmOperandsVisitorBinaryOp.apply(dst, first, second, this)
    }

    override fun rrr(dst: XmmRegister, first: XmmRegister, second: XmmRegister) {
        if (first == dst) {
            asm.xorpf(size, second, dst)
        } else if (second == dst) {
            asm.xorpf(size, first, dst)
        } else {
            asm.movf(size, first, dst)
            asm.xorpf(size, second, dst)
        }
    }

    override fun arr(dst: Address, first: XmmRegister, second: XmmRegister) {
        TODO("Not yet implemented")
    }

    override fun rar(dst: XmmRegister, first: Address, second: XmmRegister) {
        if (second == dst) {
            asm.movf(size, first, dst) // TODO
            asm.xorpf(size, second, dst)
        } else {
            asm.movf(size, first, dst)
            asm.xorpf(size, second, dst)
        }
    }

    override fun rra(dst: XmmRegister, first: XmmRegister, second: Address) {
        if (first == dst) {
            asm.movf(size, second, xmmTemp1) //TODO
            asm.xorpf(size, xmmTemp1, dst)
        } else {
            asm.movf(size, second, dst)
            asm.xorpf(size, first, dst)
        }
    }

    override fun raa(dst: XmmRegister, first: Address, second: Address) {
        asm.movf(size, first, dst)
        asm.movf(size, second, xmmTemp1) //TODO
        asm.xorpf(size, xmmTemp1, dst)
    }

    override fun ara(dst: Address, first: XmmRegister, second: Address) {
        asm.movf(size, first, xmmTemp1)
        asm.movf(size, second, fpRet)
        asm.xorpf(size, fpRet, xmmTemp1)
        asm.movf(size, xmmTemp1, dst)
    }

    override fun aar(dst: Address, first: Address, second: XmmRegister) {
        TODO("Not yet implemented")
    }

    override fun aaa(dst: Address, first: Address, second: Address) {
        asm.movf(size, first, xmmTemp1)
        asm.xorpf(size, second, xmmTemp1)
        asm.movf(size, xmmTemp1, dst)
    }

    override fun default(dst: Operand, first: Operand, second: Operand) {
        throw RuntimeException("Internal error: '${ir.instruction.Xor.NAME}' dst=$dst, first=$first, second=$second")
    }
}