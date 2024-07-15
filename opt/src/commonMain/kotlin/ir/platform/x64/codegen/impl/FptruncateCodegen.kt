package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.types.*
import common.assertion
import ir.instruction.FpTruncate
import ir.platform.x64.CallConvention.xmmTemp1
import ir.platform.x64.codegen.visitors.XmmOperandsVisitorUnaryOp


data class FptruncateCodegen(val toType: FloatingPointType, val asm: Assembler):
    XmmOperandsVisitorUnaryOp {
    private val toSize = toType.sizeOf()

    init {
        assertion(toType == Type.F32) {
            "expect this, but toType=$toType"
        }
    }

    operator fun invoke(dst: Operand, src: Operand) {
        XmmOperandsVisitorUnaryOp.apply(dst, src, this)
    }

    override fun rrF(dst: XmmRegister, src: XmmRegister) {
        asm.cvtsd2ss(src, dst)
    }

    override fun raF(dst: XmmRegister, src: Address) {
        asm.cvtsd2ss(src, dst)
    }

    override fun arF(dst: Address, src: XmmRegister) {
        asm.cvtsd2ss(src, xmmTemp1)
        asm.movf(toSize, xmmTemp1, dst)
    }

    override fun aaF(dst: Address, src: Address) {
        asm.cvtsd2ss(src, xmmTemp1)
        asm.movf(toSize, xmmTemp1, dst)
    }

    override fun default(dst: Operand, src: Operand) {
        throw RuntimeException("Internal error: '${FpTruncate.NAME}' dst=$dst, src=$src")
    }
}