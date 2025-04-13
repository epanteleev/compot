package ir.platform.x64.codegen.impl

import asm.x64.Operand
import asm.x64.*
import common.assertion
import ir.types.*
import ir.instruction.FpExtend
import ir.platform.x64.codegen.visitors.*
import ir.platform.x64.CallConvention.xmmTemp1


internal class FpExtendCodegen(val toType: FloatingPointType, val asm: Assembler):
    XmmOperandsVisitorUnaryOp {
    private val toSize = toType.sizeOf()

    init {
        assertion(toType == F64Type) {
            "expect this, but toType=$toType"
        }
    }

    operator fun invoke(dst: Operand, src: Operand) {
        XmmOperandsVisitorUnaryOp.apply(dst, src, this)
    }

    override fun rr(dst: XmmRegister, src: XmmRegister) {
        asm.cvtss2sd(src, dst)
    }

    override fun ra(dst: XmmRegister, src: Address) {
        asm.cvtss2sd(src, dst)
    }

    override fun ar(dst: Address, src: XmmRegister) {
        asm.cvtss2sd(src, xmmTemp1)
        asm.movf(toSize, xmmTemp1, dst)
    }

    override fun aa(dst: Address, src: Address) {
        asm.cvtss2sd(src, xmmTemp1)
        asm.movf(toSize, xmmTemp1, dst)
    }

    override fun default(dst: Operand, src: Operand) {
        throw RuntimeException("Internal error: '${FpExtend.NAME}' dst=$dst, src=$$src")
    }
}