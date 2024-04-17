package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.types.FloatingPointType
import ir.instruction.FloatToSigned
import ir.platform.x64.codegen.utils.*
import ir.types.SignedIntType


class FloatToSignedCodegen(val toType: SignedIntType, val fromType: FloatingPointType, val asm: Assembler): XmmToGPOperandsVisitor {
    private val toSize = toType.size()
    private val fromSize = fromType.size()

    operator fun invoke(dst: Operand, src: Operand) {
        ApplyClosure(dst, src, this)
    }

    override fun rx(dst: GPRegister, src: XmmRegister) {
        asm.cvtfp2int(toSize, fromSize, src, dst)
    }

    override fun ax(dst: Address, src: XmmRegister) {
        TODO("Not yet implemented")
    }

    override fun ra(dst: GPRegister, src: Address) {
        TODO("Not yet implemented")
    }

    override fun aa(dst: Address, src: Address) {
        TODO("Not yet implemented")
    }

    override fun default(dst: Operand, src: Operand) {
        throw RuntimeException("Internal error: '${FloatToSigned.NAME}' dst=$dst, src=$$src")
    }
}