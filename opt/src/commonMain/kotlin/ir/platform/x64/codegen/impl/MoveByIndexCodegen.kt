package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.Definitions.POINTER_SIZE
import ir.types.*
import ir.instruction.lir.Move
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.CallConvention.temp2
import ir.platform.x64.CallConvention.xmmTemp1
import ir.platform.x64.codegen.visitors.GPOperandsVisitorBinaryOp


class MoveByIndexCodegen(val type: PrimitiveType, indexType: NonTrivialType, val asm: Assembler) : GPOperandsVisitorBinaryOp {
    private val size = type.sizeOf()
    private val indexSize = indexType.sizeOf()

    operator fun invoke(dst: Operand, source: Operand, index: Operand) {
        when (type) {
            is FloatingPointType -> {
                if (dst is GPRegister && source is XmmRegister && index is GPRegister) {
                    asm.movf(size, source, Address.from(dst, 0, index, size))
                } else if (dst is GPRegister && source is XmmRegister && index is ImmInt) {
                    asm.movf(size, source, Address.from(dst, index.value().toInt() * size))
                } else if (dst is GPRegister && source is Address && index is ImmInt) {
                    asm.movf(size, source, xmmTemp1)
                    asm.movf(size, xmmTemp1, Address.from(dst, index.value().toInt() * size))
                } else {
                    default(dst, source, index)
                }
            }
            is IntegerType, is PointerType -> GPOperandsVisitorBinaryOp.apply(dst, source, index, this)
            else -> throw RuntimeException("Unknown type=$type, dst=$dst, source=$source, index=$index")
        }
    }

    override fun rrr(dst: GPRegister, first: GPRegister, second: GPRegister) {
        asm.mov(size, first, Address.from(dst, 0, second, size))
    }

    override fun arr(dst: Address, first: GPRegister, second: GPRegister) {
        TODO("Not yet implemented")
    }

    override fun rar(dst: GPRegister, first: Address, second: GPRegister) {
        TODO("Not yet implemented")
    }

    override fun rir(dst: GPRegister, first: Imm32, second: GPRegister) {
        asm.mov(size, first, temp1)
        asm.mov(size, temp1, Address.from(dst, 0, second, size))
    }

    override fun rra(dst: GPRegister, first: GPRegister, second: Address) {
        TODO("untested")
        asm.mov(indexSize, second, temp1)
        asm.mov(size, first, Address.from(dst, 0, temp1, size))
    }

    override fun rri(dst: GPRegister, first: GPRegister, second: Imm32) {
        asm.mov(size, first, Address.from(dst, second.value().toInt() * size))
    }

    override fun raa(dst: GPRegister, first: Address, second: Address) {
        TODO("Not yet implemented")
    }

    override fun rii(dst: GPRegister, first: Imm32, second: Imm32) {
        asm.mov(size, first, Address.from(dst, second.value().toInt() * size))
    }

    override fun ria(dst: GPRegister, first: Imm32, second: Address) {
        TODO("Not yet implemented")
    }

    override fun rai(dst: GPRegister, first: Address, second: Imm32) {
        TODO("Not yet implemented")
    }

    override fun ara(dst: Address, first: GPRegister, second: Address) {
        asm.mov(size, dst, temp1)
        asm.mov(size, second, temp2)
        asm.mov(size, first, Address.from(temp1, 0, temp2, size))
    }

    override fun aii(dst: Address, first: Imm32, second: Imm32) {
        TODO("untested")
        asm.mov(POINTER_SIZE, dst, temp1)
        asm.mov(size, first, temp2)
        asm.mov(size, temp2, Address.from(temp1, second.value().toInt() * size))
    }

    override fun air(dst: Address, first: Imm32, second: GPRegister) {
        TODO("Not yet implemented")
    }

    override fun aia(dst: Address, first: Imm32, second: Address) {
        asm.mov(size, dst, temp1)
        asm.mov(size, second, temp2)
        asm.mov(size, first, Address.from(temp1, 0, temp2, size))
    }

    override fun ari(dst: Address, first: GPRegister, second: Imm32) {
        TODO("untested")
        val disp = second.value() * size

        asm.mov(size, Address.from(first, disp.toInt()), temp1)
        asm.mov(size, temp1, dst)
    }

    override fun aai(dst: Address, first: Address, second: Imm32) {
        TODO("Not yet implemented")
    }

    override fun aar(dst: Address, first: Address, second: GPRegister) {
        TODO("untested")
        asm.mov(POINTER_SIZE, dst, temp1)
        asm.mov(size, first, temp2)
        asm.mov(size, temp2, Address.from(temp1, 0, second, size))
    }

    override fun aaa(dst: Address, first: Address, second: Address) {
        TODO("Not yet implemented")
    }

    override fun default(dst: Operand, first: Operand, second: Operand) {
        throw RuntimeException("Internal error: '${Move.NAME}' dst=$dst, first=$first, second=$second")
    }
}