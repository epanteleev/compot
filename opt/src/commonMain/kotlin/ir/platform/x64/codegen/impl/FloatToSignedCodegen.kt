package ir.platform.x64.codegen.impl

import asm.x64.Operand
import asm.x64.*
import ir.types.FloatingPointType
import ir.instruction.Float2Int
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.codegen.visitors.*
import ir.types.IntegerType


internal class FloatToSignedCodegen(val toType: IntegerType, fromType: FloatingPointType, val asm: Assembler): XmmToGPOperandsVisitor {
    private val toSize = run {
        val size = toType.sizeOf()
        if (size >= 4) {
            size
        } else {
            4
        }
    }

    private val fromSize = fromType.sizeOf()

    operator fun invoke(dst: Operand, src: Operand) {
        XmmToGPOperandsVisitor.apply(dst, src, this)
    }

    override fun rx(dst: GPRegister, src: XmmRegister) {
        asm.cvtfp2int(toSize, fromSize, src, dst)
    }

    override fun ax(dst: Address, src: XmmRegister) {
        asm.cvtfp2int(toSize, fromSize, src, temp1)
        asm.mov(toSize, temp1, dst)
    }

    override fun ra(dst: GPRegister, src: Address) {
        asm.cvtfp2int(toSize, fromSize, src, dst)
    }

    override fun aa(dst: Address, src: Address) {
        TODO("Not yet implemented")
    }

    override fun default(dst: Operand, src: Operand) {
        throw RuntimeException("Internal error: '${Float2Int.NAME}' dst=$dst, src=$$src")
    }
}