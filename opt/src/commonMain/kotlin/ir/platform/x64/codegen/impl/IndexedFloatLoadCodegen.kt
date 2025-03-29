package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.types.*
import asm.Operand
import ir.Definitions.POINTER_SIZE
import ir.instruction.lir.IndexedLoad
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.CallConvention.xmmTemp1


class IndexedFloatLoadCodegen(loadedType: FloatingPointType, indexType: PrimitiveType, val asm: Assembler) {
    private val size: Int = loadedType.sizeOf()
    private val indexSize: Int = indexType.sizeOf()

    operator fun invoke(dst: Operand, operand: Operand, index: Operand) {
        handleXmm(dst, operand, index)
    }

    private fun handleXmm(dst: Operand, operand: Operand, index: Operand) {
        if (dst is XmmRegister && operand is GPRegister && index is GPRegister) {
            asm.movf(size, Address.from(operand, 0, index, ScaleFactor.from(size)), dst)

        } else if (dst is XmmRegister && operand is GPRegister && index is ImmInt) {
            asm.movf(size, Address.from(operand, index.value().toInt() * size), dst)

        } else if (dst is XmmRegister && operand is GPRegister && index is Address) {
            asm.mov(indexSize, index, temp1)
            asm.movf(size, Address.from(operand, 0, temp1, ScaleFactor.from(size)), dst)

        } else if (dst is XmmRegister && operand is Address && index is ImmInt) {
            asm.mov(POINTER_SIZE, operand, temp1)
            asm.movf(size, Address.from(temp1, index.value().toInt() * size), dst)

        } else if (dst is Address && operand is GPRegister && index is ImmInt) {
            asm.movf(size, Address.from(operand, index.value().toInt() * size), xmmTemp1)
            asm.movf(size, xmmTemp1, dst)

        } else if (dst is Address && operand is Address && index is ImmInt) {
            asm.mov(POINTER_SIZE, operand, temp1)
            asm.movf(size, Address.from(temp1, index.value().toInt() * size), xmmTemp1)
            asm.movf(size, xmmTemp1, dst)

        } else {
            default(dst, operand, index)
        }
    }

    private fun default(dst: Operand, first: Operand, second: Operand) {
        throw RuntimeException("Internal error: '${IndexedLoad.NAME}', dst=$dst, operand=$first, index=$second")
    }
}