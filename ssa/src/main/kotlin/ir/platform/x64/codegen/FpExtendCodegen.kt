package ir.platform.x64.codegen

import asm.x64.*
import ir.types.*
import ir.platform.x64.utils.*
import ir.instruction.FpExtend
import ir.platform.x64.CallConvention.xmmTemp1


data class FpExtendCodegen (val toType: FloatingPointType, val asm: Assembler):
    XmmOperandVisitorUnaryOp {
    private val toSize = toType.size()

    init {
        assert(toType == Type.F64) {
            "expect this, but toType=$toType"
        }
    }

    operator fun invoke(dst: Operand, src: Operand) {
        ApplyClosure(dst, src, this as XmmOperandVisitorUnaryOp)
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