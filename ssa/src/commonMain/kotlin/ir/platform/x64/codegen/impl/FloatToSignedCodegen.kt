package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.types.FloatingPointType
import ir.instruction.FloatToInt
import ir.platform.x64.codegen.visitors.*
import ir.types.IntegerType


class FloatToSignedCodegen(val toType: IntegerType, fromType: FloatingPointType, val asm: Assembler): XmmToGPOperandsVisitor {
    private val toSize = run {
        val size = toType.size()
        if (size >= 4) {
            size
        } else {
            4
        }
    }

    private val fromSize = fromType.size()

    operator fun invoke(dst: Operand, src: Operand) {
        XmmToGPOperandsVisitor.apply(dst, src, this)
    }

    override fun rx(dst: GPRegister, src: XmmRegister) {
        asm.cvtfp2int(toSize, fromSize, src, dst)
    }

    override fun ax(dst: Address, src: XmmRegister) {
        TODO("Not yet implemented")
    }

    override fun ra(dst: GPRegister, src: Address) {
        asm.cvtfp2int(toSize, fromSize, src, dst)
    }

    override fun aa(dst: Address, src: Address) {
        TODO("Not yet implemented")
    }

    override fun default(dst: Operand, src: Operand) {
        throw RuntimeException("Internal error: '${FloatToInt.NAME}' dst=$dst, src=$$src")
    }
}