package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.types.*
import asm.x64.Operand
import ir.Definitions.POINTER_SIZE
import ir.instruction.lir.IndexedLoad
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.CallConvention.temp2
import ir.platform.x64.CallConvention.xmmTemp1


class IndexedFloatLoadCodegen(loadedType: FloatingPointType, indexType: PrimitiveType, val asm: Assembler) {
    private val size: Int = loadedType.sizeOf()
    private val indexSize: Int = indexType.sizeOf()

    operator fun invoke(dst: Operand, operand: Operand, index: Operand) {
        handleXmm(dst, operand, index)
    }

    private fun handleXmm(dst: Operand, operand: Operand, index: Operand) {
        when (dst) {
            is XmmRegister if operand is GPRegister && index is GPRegister -> {
                asm.movf(size, Address.from(operand, 0, index, ScaleFactor.from(size)), dst)
            }
            is XmmRegister if operand is Address && index is GPRegister -> {
                asm.mov(POINTER_SIZE, operand, temp1)
                asm.movf(size, Address.from(temp1, 0, index, ScaleFactor.from(size)), dst)
            }
            is XmmRegister if operand is Address && index is Address -> {
                asm.mov(POINTER_SIZE, operand, temp1)
                asm.mov(indexSize, index, temp2)
                asm.movf(size, Address.from(temp1, 0, temp2, ScaleFactor.from(size)), dst)
            }
            is XmmRegister if operand is GPRegister && index is Imm -> {
                asm.movf(size, Address.from(operand, index.value().toInt() * size), dst)
            }
            is XmmRegister if operand is GPRegister && index is Address -> {
                asm.mov(indexSize, index, temp1)
                asm.movf(size, Address.from(operand, 0, temp1, ScaleFactor.from(size)), dst)
            }
            is XmmRegister if operand is Address && index is Imm -> {
                asm.mov(POINTER_SIZE, operand, temp1)
                asm.movf(size, Address.from(temp1, index.value().toInt() * size), dst)
            }
            is Address if operand is GPRegister && index is Imm -> {
                asm.movf(size, Address.from(operand, index.value().toInt() * size), xmmTemp1)
                asm.movf(size, xmmTemp1, dst)
            }
            is Address if operand is Address && index is Imm -> {
                asm.mov(POINTER_SIZE, operand, temp1)
                asm.movf(size, Address.from(temp1, index.value().toInt() * size), xmmTemp1)
                asm.movf(size, xmmTemp1, dst)
            }
            is Address if operand is GPRegister && index is Address -> {
                asm.mov(indexSize, index, temp1)
                asm.movf(size, Address.from(temp1, 0, operand, ScaleFactor.from(size)), xmmTemp1)
                asm.movf(size, xmmTemp1, dst)
            }
            is Address if operand is Address && index is Address -> {
                asm.mov(POINTER_SIZE, operand, temp1)
                asm.mov(indexSize, index, temp2)
                asm.movf(size, Address.from(temp2, 0, temp1, ScaleFactor.from(size)), xmmTemp1)
                asm.movf(size, xmmTemp1, dst)
            }
            else -> default(dst, operand, index)
        }
    }

    private fun default(dst: Operand, first: Operand, second: Operand) {
        throw RuntimeException("Internal error: '${IndexedLoad.NAME}', dst=$dst, operand=$first, index=$second")
    }
}