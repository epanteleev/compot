package ir.platform.x64.codegen.impl

import asm.x64.*
import common.assertion
import ir.types.*
import ir.instruction.FpExtend
import ir.platform.x64.codegen.visitors.*
import ir.platform.x64.CallConvention.xmmTemp1


data class FpExtendCodegen(val toType: FloatingPointType, val asm: Assembler):
    XmmOperandsVisitorUnaryOp {
    private val toSize = toType.sizeOf()

    init {
        assertion(toType == Type.F64) {
            "expect this, but toType=$toType"
        }
    }

    operator fun invoke(dst: Operand, src: Operand) {
        XmmOperandsVisitorUnaryOp.apply(dst, src, this)
    }

    override fun rrF(dst: XmmRegister, src: XmmRegister) {
        asm.cvtss2sd(src, dst)
    }

    override fun raF(dst: XmmRegister, src: Address) {
        asm.cvtss2sd(src, dst)
    }

    override fun arF(dst: Address, src: XmmRegister) {
        asm.cvtss2sd(src, xmmTemp1)
        asm.movf(toSize, xmmTemp1, dst)
    }

    override fun aaF(dst: Address, src: Address) {
        asm.cvtss2sd(src, xmmTemp1)
        asm.movf(toSize, xmmTemp1, dst)
    }

    override fun default(dst: Operand, src: Operand) {
        throw RuntimeException("Internal error: '${FpExtend.NAME}' dst=$dst, src=$$src")
    }
}