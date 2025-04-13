package ir.platform.x64.codegen.impl

import asm.x64.Operand
import asm.x64.*
import ir.types.*
import common.assertion
import ir.instruction.FpTruncate
import ir.platform.x64.CallConvention.xmmTemp1
import ir.platform.x64.codegen.visitors.XmmOperandsVisitorUnaryOp


internal class FptruncateCodegen(val toType: FloatingPointType, val asm: Assembler):
    XmmOperandsVisitorUnaryOp {
    private val toSize = toType.sizeOf()

    init {
        assertion(toType == F32Type) {
            "expect this, but toType=$toType"
        }
    }

    operator fun invoke(dst: Operand, src: Operand) {
        XmmOperandsVisitorUnaryOp.apply(dst, src, this)
    }

    override fun rr(dst: XmmRegister, src: XmmRegister) {
        asm.cvtsd2ss(src, dst)
    }

    override fun ra(dst: XmmRegister, src: Address) {
        asm.cvtsd2ss(src, dst)
    }

    override fun ar(dst: Address, src: XmmRegister) {
        asm.cvtsd2ss(src, xmmTemp1)
        asm.movf(toSize, xmmTemp1, dst)
    }

    override fun aa(dst: Address, src: Address) {
        asm.cvtsd2ss(src, xmmTemp1)
        asm.movf(toSize, xmmTemp1, dst)
    }

    override fun default(dst: Operand, src: Operand) {
        throw RuntimeException("Internal error: '${FpTruncate.NAME}' dst=$dst, src=$src")
    }
}