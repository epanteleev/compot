package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.types.*
import ir.instruction.lir.IndexedLoad
import ir.platform.x64.codegen.visitors.*


class IndexedLoadCodegen(private val loadedType: PrimitiveType, val asm: Assembler): GPOperandsVisitorBinaryOp {
    private val size: Int = loadedType.size()

    operator fun invoke(dst: Operand, operand: Operand, index: Operand) {
        when (loadedType) {
            is FloatingPointType -> handleXmm(dst, operand, index)
            is IntegerType, is PointerType -> GPOperandsVisitorBinaryOp.apply(dst, operand, index, this)
            else           -> default(dst, operand, index)
        }
    }

    private fun handleXmm(dst: Operand, operand: Operand, index: Operand) {
        if (dst is XmmRegister && operand is GPRegister && index is GPRegister) {
            asm.movf(size, Address.from(operand, 0, index, loadedType.size()), dst)
        } else if (dst is XmmRegister && operand is GPRegister && index is ImmInt) {
            asm.movf(size, Address.from(operand, index.value().toInt() * loadedType.size()), dst)
        } else {
            default(dst, operand, index)
        }
    }

    override fun rrr(dst: GPRegister, first: GPRegister, second: GPRegister) {
        asm.mov(size, Address.from(first, 0, second, loadedType.size()), dst)
    }

    override fun arr(dst: Address, first: GPRegister, second: GPRegister) {
        TODO("Not yet implemented")
    }

    override fun rar(dst: GPRegister, first: Address, second: GPRegister) {
        TODO("Not yet implemented")
    }

    override fun rir(dst: GPRegister, first: Imm32, second: GPRegister) {
        TODO("Not yet implemented")
    }

    override fun rra(dst: GPRegister, first: GPRegister, second: Address) {
        TODO("Not yet implemented")
    }

    override fun rri(dst: GPRegister, first: GPRegister, second: Imm32) {
        asm.mov(size, Address.from(first, second.value().toInt() * loadedType.size()), dst)
    }

    override fun raa(dst: GPRegister, first: Address, second: Address) {
        TODO("Not yet implemented")
    }

    override fun rii(dst: GPRegister, first: Imm32, second: Imm32) = default(dst, first, second)

    override fun ria(dst: GPRegister, first: Imm32, second: Address) {
        TODO("Not yet implemented")
    }

    override fun rai(dst: GPRegister, first: Address, second: Imm32) {
        TODO("Not yet implemented")
    }

    override fun ara(dst: Address, first: GPRegister, second: Address) {
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
    }

    override fun aai(dst: Address, first: Address, second: Imm32) {
        TODO("Not yet implemented")
    }

    override fun aar(dst: Address, first: Address, second: GPRegister) {
        TODO("Not yet implemented")
    }

    override fun aaa(dst: Address, first: Address, second: Address) {
        TODO("Not yet implemented")
    }

    override fun default(dst: Operand, first: Operand, second: Operand) {
        throw RuntimeException("Internal error: '${IndexedLoad.NAME}', dst=$dst, operand=$first, index=$second")
    }
}