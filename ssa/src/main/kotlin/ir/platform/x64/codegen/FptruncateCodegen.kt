package ir.platform.x64.codegen

import asm.x64.*
import ir.platform.x64.utils.*
import ir.instruction.Fptruncate
import ir.types.FloatingPointType
import ir.types.Type


data class FptruncateCodegen(val fromType: FloatingPointType, val toType: FloatingPointType, val asm: Assembler):
    XmmOperandVisitorUnaryOp {
    init {
        assert(toType == Type.F32) {
            "extect this, but toType=$toType"
        }
    }

    operator fun invoke(dst: Operand, src: Operand) {
        ApplyClosure(dst, src, this as XmmOperandVisitorUnaryOp)
    }

    override fun rrF(dst: XmmRegister, src: XmmRegister) {
        asm.cvtsd2ss(src, dst)
    }

    override fun raF(dst: XmmRegister, src: Address) {
        TODO("Not yet implemented")
    }

    override fun arF(dst: Address, src: XmmRegister) {
        TODO("Not yet implemented")
    }

    override fun aaF(dst: Address, src: Address) {
        TODO("Not yet implemented")
    }

    override fun default(dst: Operand, src: Operand) {
        throw RuntimeException("Internal error: '${Fptruncate.NAME}' dst=$dst, src=$$src")
    }
}