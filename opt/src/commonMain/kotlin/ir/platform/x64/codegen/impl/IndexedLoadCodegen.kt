package ir.platform.x64.codegen.impl

import asm.Operand
import asm.x64.*
import ir.Definitions.POINTER_SIZE
import ir.types.*
import ir.instruction.lir.IndexedLoad
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.CallConvention.temp2
import ir.platform.x64.CallConvention.xmmTemp1
import ir.platform.x64.codegen.visitors.*


class IndexedLoadCodegen(private val loadedType: PrimitiveType, indexType: PrimitiveType, val asm: Assembler): GPOperandsVisitorBinaryOp {
    private val size: Int = loadedType.sizeOf()
    private val indexSize: Int = indexType.sizeOf()

    operator fun invoke(dst: Operand, operand: Operand, index: Operand) = when (loadedType) {
        is FloatingPointType -> handleXmm(dst, operand, index)
        is IntegerType, is PtrType -> GPOperandsVisitorBinaryOp.apply(dst, operand, index, this)
        else -> default(dst, operand, index)
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

    override fun rrr(dst: GPRegister, first: GPRegister, second: GPRegister) {
        asm.mov(size, Address.from(first, 0, second, ScaleFactor.from(size)), dst)
    }

    override fun arr(dst: Address, first: GPRegister, second: GPRegister) {
        asm.mov(size, Address.from(first, 0, second, ScaleFactor.from(size)), temp1)
        asm.mov(size, temp1, dst)
    }

    override fun rar(dst: GPRegister, first: Address, second: GPRegister) {
        asm.mov(POINTER_SIZE, first, temp1)
        asm.mov(size, Address.from(temp1, 0, second, ScaleFactor.from(size)), dst)
    }

    override fun rir(dst: GPRegister, first: Imm32, second: GPRegister) {
        TODO("Not yet implemented")
    }

    override fun rra(dst: GPRegister, first: GPRegister, second: Address) {
        asm.mov(indexSize, second, temp1)
        asm.mov(size, Address.from(first, 0, temp1, ScaleFactor.from(size)), dst)
    }

    override fun rri(dst: GPRegister, first: GPRegister, second: Imm32) {
        asm.mov(size, Address.from(first, second.value().toInt() * size), dst)
    }

    override fun raa(dst: GPRegister, first: Address, second: Address) {
        asm.mov(indexSize, second, temp1)
        asm.mov(POINTER_SIZE, first, temp2)
        asm.mov(size, Address.from(temp2, 0, temp1, ScaleFactor.from(size)), dst)
    }

    override fun rii(dst: GPRegister, first: Imm32, second: Imm32) = default(dst, first, second)

    override fun ria(dst: GPRegister, first: Imm32, second: Address) {
        TODO("Not yet implemented")
    }

    override fun rai(dst: GPRegister, first: Address, second: Imm32) {
        asm.mov(POINTER_SIZE, first, temp1)
        asm.mov(size, Address.from(temp1, second.value().toInt() * size), dst)
    }

    override fun ara(dst: Address, first: GPRegister, second: Address) {
        asm.mov(indexSize, second, temp1)
        asm.mov(size, Address.from(first, 0, temp1, ScaleFactor.from(size)), temp1)
        asm.mov(size, temp1, dst)
    }

    override fun aii(dst: Address, first: Imm32, second: Imm32) {
        TODO("Not yet implemented")
    }

    override fun air(dst: Address, first: Imm32, second: GPRegister) {
        TODO("Not yet implemented")
    }

    override fun aia(dst: Address, first: Imm32, second: Address) {
        TODO("Not yet implemented")
    }

    override fun ari(dst: Address, first: GPRegister, second: Imm32) {
        asm.mov(size, Address.from(first, second.value().toInt() * size), temp1)
        asm.mov(size, temp1, dst)
    }

    override fun aai(dst: Address, first: Address, second: Imm32) {
        asm.mov(POINTER_SIZE, first, temp1)
        asm.mov(size, Address.from(temp1, second.value().toInt() * size), temp1)
        asm.mov(size, temp1, dst)
    }

    override fun aar(dst: Address, first: Address, second: GPRegister) {
        asm.mov(POINTER_SIZE, first, temp1)
        asm.mov(size, Address.from(temp1, 0, second, ScaleFactor.from(size)), temp1)
        asm.mov(size, temp1, dst)
    }

    override fun aaa(dst: Address, first: Address, second: Address) {
        asm.mov(indexSize, second, temp1)
        asm.mov(POINTER_SIZE, first, temp2)
        asm.mov(size, Address.from(temp2, 0, temp1, ScaleFactor.from(size)), temp1)
        asm.mov(size, temp1, dst)
    }

    override fun default(dst: Operand, first: Operand, second: Operand) {
        throw RuntimeException("Internal error: '${IndexedLoad.NAME}', dst=$dst, operand=$first, index=$second")
    }
}