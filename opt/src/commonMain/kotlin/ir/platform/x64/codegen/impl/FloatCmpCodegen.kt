package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.types.FloatingPointType
import ir.instruction.FloatCompare
import ir.platform.x64.CallConvention.xmmTemp1
import ir.platform.x64.codegen.visitors.CmpXmmOperandVisitor


class FloatCmpCodegen(val type: FloatingPointType, val asm: Assembler): CmpXmmOperandVisitor {
    private val size: Int = type.sizeOf()

    operator fun invoke(first: Operand, second: Operand) {
        CmpXmmOperandVisitor.apply(first, second, this)
    }

    override fun rr(first: XmmRegister, second: XmmRegister) {
        asm.cmpf(size, second, first)
    }

    override fun ra(first: XmmRegister, second: Address) {
        asm.cmpf(size, second, first)
    }

    override fun ar(first: Address, second: XmmRegister) {
        asm.movf(size, first, xmmTemp1)
        asm.cmpf(size, second, xmmTemp1)
    }

    override fun aa(first: Address, second: Address) {
        asm.movf(size, first, xmmTemp1)
        asm.cmpf(size, second, xmmTemp1)
    }

    override fun default(first: Operand, second: Operand) {
        throw RuntimeException("Internal error: '${FloatCompare.NAME}' first=$first, second=$second")
    }
}