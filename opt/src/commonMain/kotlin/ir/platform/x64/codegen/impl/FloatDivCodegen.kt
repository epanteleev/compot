package ir.platform.x64.codegen.impl

import asm.x64.Operand
import asm.x64.*
import ir.platform.x64.CallConvention.xmmTemp1
import ir.types.ArithmeticType
import ir.platform.x64.codegen.visitors.XmmOperandsVisitorBinaryOp


internal class FloatDivCodegen(val type: ArithmeticType, val asm: Assembler): XmmOperandsVisitorBinaryOp {
    private val size: Int = type.sizeOf()

    operator fun invoke(dst: Operand, first: Operand, second: Operand) {
        XmmOperandsVisitorBinaryOp.apply(dst, first, second, this)
    }

    override fun rrr(dst: XmmRegister, first: XmmRegister, second: XmmRegister) {
        if (first == dst) {
            asm.divf(size, second, dst)
        } else if (second == dst) {
            asm.movf(size, first, xmmTemp1) //TODO
            asm.divf(size, second, xmmTemp1)
            asm.movf(size, xmmTemp1, dst)
        } else {
            asm.movf(size, first, dst)
            asm.divf(size, second, dst)
        }
    }

    override fun arr(dst: Address, first: XmmRegister, second: XmmRegister) {
        asm.movf(size, first, xmmTemp1) //TODO
        asm.divf(size, second, xmmTemp1)
        asm.movf(size, xmmTemp1, dst)
    }

    override fun rar(dst: XmmRegister, first: Address, second: XmmRegister) {
        if (second == dst) {
            asm.movf(size, first, xmmTemp1) //TODO
            asm.divf(size, second, xmmTemp1)
            asm.movf(size, xmmTemp1, dst)
        } else {
            asm.movf(size, first, dst)
            asm.divf(size, second, dst)
        }
    }

    override fun rra(dst: XmmRegister, first: XmmRegister, second: Address) {
        if (dst == first) {
            asm.divf(size, second, dst)
        } else {
            asm.movf(size, first, dst)
            asm.divf(size, second, dst)
        }
    }

    override fun raa(dst: XmmRegister, first: Address, second: Address) {
        asm.movf(size, first, dst)
        asm.divf(size, second, dst)
    }

    override fun ara(dst: Address, first: XmmRegister, second: Address) {
        asm.movf(size, first, xmmTemp1)
        asm.divf(size, second, xmmTemp1)
        asm.movf(size, xmmTemp1, dst)
    }

    override fun aar(dst: Address, first: Address, second: XmmRegister) {
        asm.movf(size, first, xmmTemp1)
        asm.divf(size, second, xmmTemp1)
        asm.movf(size, xmmTemp1, dst)
    }

    override fun aaa(dst: Address, first: Address, second: Address) {
        asm.movf(size, first, xmmTemp1) //TODO
        asm.divf(size, second, xmmTemp1)
        asm.movf(size, xmmTemp1, dst)
    }

    override fun default(dst: Operand, first: Operand, second: Operand) {
        throw RuntimeException("Internal error: '${ir.instruction.Div.NAME}' dst=$dst, first=$first, second=$second")
    }
}