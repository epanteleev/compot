package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.types.*
import ir.instruction.FpTruncate
import ir.platform.x64.CallConvention.xmmTemp1
import ir.platform.x64.codegen.utils.XmmOperandsVisitorUnaryOp


data class FptruncateCodegen(val toType: FloatingPointType, val asm: Assembler):
    XmmOperandsVisitorUnaryOp {
    private val toSize = toType.size()

    init {
        assert(toType == Type.F32) {
            "expect this, but toType=$toType"
        }
    }

    operator fun invoke(dst: Operand, src: Operand) {
        ir.platform.x64.codegen.utils.ApplyClosure(dst, src, this as XmmOperandsVisitorUnaryOp)
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