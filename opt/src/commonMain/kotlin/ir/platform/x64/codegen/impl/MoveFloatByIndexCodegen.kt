package ir.platform.x64.codegen.impl

import asm.x64.*
import asm.Operand
import ir.Definitions.POINTER_SIZE
import ir.instruction.lir.MoveByIndex
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.CallConvention.xmmTemp1
import ir.types.FloatingPointType


internal class MoveFloatByIndexCodegen(type: FloatingPointType, val asm: Assembler) {
    private val size = type.sizeOf()

    operator fun invoke(dst: Operand, source: Operand, index: Operand) {
        if (dst is GPRegister && source is XmmRegister && index is GPRegister) {
            asm.movf(size, source, Address.from(dst, 0, index, ScaleFactor.from(size)))

        } else if (dst is GPRegister && source is XmmRegister && index is ImmInt) {
            asm.movf(size, source, Address.from(dst, index.value().toInt() * size))

        } else if (dst is GPRegister && source is Address && index is ImmInt) {
            asm.movf(size, source, xmmTemp1)
            asm.movf(size, xmmTemp1, Address.from(dst, index.value().toInt() * size))

        } else if (dst is Address && source is XmmRegister && index is ImmInt) {
            asm.mov(POINTER_SIZE, dst, temp1)
            asm.movf(size, source, Address.from(temp1, index.value().toInt() * size))

        } else if (dst is Address && source is Address && index is ImmInt) {
            asm.mov(POINTER_SIZE, dst, temp1)
            asm.movf(size, source, xmmTemp1)
            asm.movf(size, xmmTemp1, Address.from(temp1, index.value().toInt() * size))

        } else {
            default(dst, source, index)
        }
    }

    private fun default(dst: Operand, first: Operand, second: Operand) {
        throw RuntimeException("Internal error: '${MoveByIndex.NAME}' dst=$dst, first=$first, second=$second")
    }
}