package ir.platform.x64.codegen.impl

import asm.Operand
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
                    asm.movf(size, source, Address.from(dst, 0, index, ScaleFactor.from(size)))
                } else if (dst is GPRegister && source is XmmRegister && index is ImmInt) {
                    asm.movf(size, source, Address.from(dst, index.value().toInt() * size))
                } else if (dst is GPRegister && source is Address && index is ImmInt) {
                    asm.movf(size, source, xmmTemp1)
                    asm.movf(size, xmmTemp1, Address.from(dst, index.value().toInt() * size))
                } else if (dst is Address && source is XmmRegister && index is ImmInt) {
                    asm.mov(size, dst, temp1)
                    asm.movf(size, source, Address.from(temp1, index.value().toInt() * size))
                } else {
                    default(dst, source, index)
                }
            }
            is IntegerType, is PtrType -> GPOperandsVisitorBinaryOp.apply(dst, source, index, this)
            else -> throw RuntimeException("Unknown type=$type, dst=$dst, source=$source, index=$index")
        }
    }

    override fun rrr(dst: GPRegister, first: GPRegister, second: GPRegister) {
        asm.mov(size, first, Address.from(dst, 0, second, ScaleFactor.from(size)))
    }

    override fun arr(dst: Address, first: GPRegister, second: GPRegister) {
        asm.mov(POINTER_SIZE, dst, temp1)
        asm.mov(size, first, Address.from(temp1, 0, second, ScaleFactor.from(size)))
    }

    override fun rar(dst: GPRegister, first: Address, second: GPRegister) {
        asm.mov(size, first, temp1)
        asm.mov(size, temp1, Address.from(dst, 0, second, ScaleFactor.from(size)))
    }

    override fun rir(dst: GPRegister, first: Imm32, second: GPRegister) {
        asm.mov(size, first, Address.from(dst, 0, second, ScaleFactor.from(size)))
    }

    override fun rra(dst: GPRegister, first: GPRegister, second: Address) {
        asm.mov(indexSize, second, temp1)
        asm.mov(size, first, Address.from(dst, 0, temp1, ScaleFactor.from(size)))
    }

    override fun rri(dst: GPRegister, first: GPRegister, second: Imm32) {
        asm.mov(size, first, Address.from(dst, second.value().toInt() * size))
    }

    override fun raa(dst: GPRegister, first: Address, second: Address) {
        asm.mov(size, first, temp1)
        asm.mov(indexSize, second, temp2)
        asm.mov(size, temp1, Address.from(dst, 0, temp2, ScaleFactor.from(size)))
    }

    override fun rii(dst: GPRegister, first: Imm32, second: Imm32) {
        asm.mov(size, first, Address.from(dst, second.value().toInt() * size))
    }

    override fun ria(dst: GPRegister, first: Imm32, second: Address) {
        asm.mov(indexSize, second, temp1)
        asm.mov(size, first, Address.from(dst, 0, temp1, ScaleFactor.from(size)))
    }

    override fun rai(dst: GPRegister, first: Address, second: Imm32) {
        asm.mov(size, first, temp1)
        asm.mov(size, temp1, Address.from(dst, second.value().toInt() * size))
    }

    override fun ara(dst: Address, first: GPRegister, second: Address) {
        asm.mov(POINTER_SIZE, dst, temp1)
        asm.mov(indexSize, second, temp2)
        asm.mov(size, first, Address.from(temp1, 0, temp2, ScaleFactor.from(size)))
    }

    override fun aii(dst: Address, first: Imm32, second: Imm32) {
        TODO("Not yet implemented")
        //asm.mov(size, first, dst.withOffset(second.value().toInt() * size))
    }

    override fun air(dst: Address, first: Imm32, second: GPRegister) {
        TODO()
    }

    override fun aia(dst: Address, first: Imm32, second: Address) {
        TODO()
    }

    override fun ari(dst: Address, first: GPRegister, second: Imm32) {
        asm.mov(POINTER_SIZE, dst, temp1)
        asm.mov(size, first, Address.from(temp1, second.value().toInt() * size))
    }

    override fun aai(dst: Address, first: Address, second: Imm32) {
        asm.mov(POINTER_SIZE, dst, temp1)
        asm.mov(size, first, temp2)
        asm.mov(size, temp2, Address.from(temp1, second.value().toInt() * size))
    }

    override fun aar(dst: Address, first: Address, second: GPRegister) {
        asm.mov(POINTER_SIZE, dst, temp1)
        asm.lea(POINTER_SIZE, Address.from(temp1, 0, second, ScaleFactor.from(size)), temp1)
        asm.mov(size, first, temp2)
        asm.mov(size, temp2, Address.from(temp1, 0))
    }

    override fun aaa(dst: Address, first: Address, second: Address) {
        asm.mov(POINTER_SIZE, dst, temp1)
        asm.mov(indexSize, second, temp2)
        asm.lea(POINTER_SIZE, Address.from(temp1, 0, temp2, ScaleFactor.from(size)), temp1)
        asm.mov(size, first, temp2)
        asm.mov(size, temp2, Address.from(temp1, 0))
    }

    override fun default(dst: Operand, first: Operand, second: Operand) {
        throw RuntimeException("Internal error: '${Move.NAME}' dst=$dst, first=$first, second=$second")
    }
}