package ir.platform.x64.codegen.impl

import asm.x64.*
import asm.x64.Operand
import ir.Definitions.POINTER_SIZE
import ir.instruction.lir.MoveByIndex
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.CallConvention.temp2
import ir.platform.x64.CallConvention.xmmTemp1
import ir.types.FloatingPointType
import ir.types.IntegerType


internal class MoveFloatByIndexCodegen(type: FloatingPointType, indexType: IntegerType, val asm: Assembler) {
    private val size = type.sizeOf()
    private val indexSize = indexType.sizeOf()

    operator fun invoke(dst: Operand, source: Operand, index: Operand) {
        when (dst) {
            is GPRegister if source is XmmRegister && index is GPRegister -> {
                asm.movf(size, source, Address.from(dst, 0, index, ScaleFactor.from(size)))
            }
            is Address if source is XmmRegister && index is GPRegister -> {
                asm.mov(POINTER_SIZE, dst, temp1)
                asm.movf(size, source, Address.from(temp1, 0, index, ScaleFactor.from(size)))
            }
            is Address if source is XmmRegister && index is Address -> {
                asm.mov(POINTER_SIZE, dst, temp1)
                asm.mov(indexSize, index, temp2)
                asm.movf(size, source, Address.from(temp1, 0, temp2, ScaleFactor.from(size)))
            }
            is GPRegister if source is XmmRegister && index is Imm -> {
                asm.movf(size, source, Address.from(dst, index.value().toInt() * size))
            }
            is GPRegister if source is Address && index is Imm -> {
                asm.movf(size, source, xmmTemp1)
                asm.movf(size, xmmTemp1, Address.from(dst, index.value().toInt() * size))
            }
            is GPRegister if source is Address && index is GPRegister -> {
                asm.movf(size, source, xmmTemp1)
                asm.movf(size, xmmTemp1, Address.from(dst, 0, index, ScaleFactor.from(size)))
            }
            is GPRegister if source is XmmRegister && index is Address -> {
                asm.mov(indexSize, index, temp1)
                asm.movf(size, source, Address.from(dst, 0, temp1, ScaleFactor.from(size)))
            }
            is GPRegister if source is Address && index is Address -> {
                asm.mov(indexSize, index, temp1)
                asm.movf(size, source, xmmTemp1)
                asm.movf(size, xmmTemp1, Address.from(dst, 0, temp1, ScaleFactor.from(size)))
            }
            is Address if source is XmmRegister && index is Imm -> {
                asm.mov(POINTER_SIZE, dst, temp1)
                asm.movf(size, source, Address.from(temp1, index.value().toInt() * size))
            }
            is Address if source is Address && index is Imm -> {
                asm.mov(POINTER_SIZE, dst, temp1)
                asm.movf(size, source, xmmTemp1)
                asm.movf(size, xmmTemp1, Address.from(temp1, index.value().toInt() * size))
            }
            is Address if source is Address && index is GPRegister -> {
                asm.mov(POINTER_SIZE, dst, temp1)
                asm.movf(size, source, xmmTemp1)
                asm.movf(size, xmmTemp1, Address.from(temp1, 0, index, ScaleFactor.from(size)))
            }
            is Address if source is Address && index is Address -> {
                asm.mov(POINTER_SIZE, dst, temp1)
                asm.mov(indexSize, index, temp2)
                asm.movf(size, source, xmmTemp1)
                asm.movf(size, xmmTemp1, Address.from(temp1, 0, temp2, ScaleFactor.from(size)))
            }
            else -> default(dst, source, index)
        }
    }

    private fun default(dst: Operand, first: Operand, second: Operand) {
        throw RuntimeException("Internal error: '${MoveByIndex.NAME}' dst=$dst, first=$first, second=$second")
    }
}