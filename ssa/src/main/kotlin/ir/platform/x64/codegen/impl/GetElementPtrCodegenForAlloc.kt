package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.types.*
import ir.instruction.GetElementPtr
import ir.platform.x64.CallConvention.POINTER_SIZE
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.codegen.utils.ApplyClosure
import ir.platform.x64.codegen.utils.GPOperandsVisitorBinaryOp


data class GetElementPtrCodegenForAlloc (val type: PointerType, val basicType: PrimitiveType, val asm: Assembler):
    GPOperandsVisitorBinaryOp {
    private val size: Int = basicType.size()

    operator fun invoke(dst: Operand, source: Operand, index: Operand) {
        ApplyClosure(dst, source, index, this as GPOperandsVisitorBinaryOp)
    }

    override fun rar(dst: GPRegister, first: Address, second: GPRegister) {
        val address = when (first) {
            is Address2 -> Address.from(first.base, first.offset, second, size)
            else -> TODO()
        }
        asm.lea(POINTER_SIZE, address, dst)
    }

    override fun raa(dst: GPRegister, first: Address, second: Address) {
        asm.mov(POINTER_SIZE, second, dst)
        val address = when (first) {
            is Address2 -> Address.from(first.base, first.offset, dst, size)
            else -> TODO()
        }
        asm.lea(POINTER_SIZE, address, dst)
    }

    override fun rai(dst: GPRegister, first: Address, second: Imm32) {
        val address = when (first) {
            is Address2 -> Address.from(first.base, first.offset + (second.value() * size).toInt()) //TODO int overflow leads to error
            else -> TODO()
        }
        asm.lea(POINTER_SIZE, address, dst)
    }

    override fun aai(dst: Address, first: Address, second: Imm32) {
        val address = when (first) {
            is Address2 -> Address.from(first.base, first.offset + (second.value() * size).toInt()) //TODO int overflow leads to error
            else -> TODO()
        }
        asm.lea(POINTER_SIZE, address, temp1)
        asm.mov(POINTER_SIZE, temp1, dst)
    }

    override fun aar(dst: Address, first: Address, second: GPRegister) {
        TODO("Not yet implemented")
    }

    override fun aaa(dst: Address, first: Address, second: Address) {
        TODO("Not yet implemented")
    }

    override fun ara(dst: Address, first: GPRegister, second: Address) {
        default(dst, first, second)
    }

    override fun aii(dst: Address, first: Imm32, second: Imm32) {
        default(dst, first, second)
    }

    override fun air(dst: Address, first: Imm32, second: GPRegister) {
        default(dst, first, second)
    }

    override fun aia(dst: Address, first: Imm32, second: Address) {
        default(dst, first, second)
    }

    override fun ari(dst: Address, first: GPRegister, second: Imm32) {
        default(dst, first, second)
    }

    override fun rii(dst: GPRegister, first: Imm32, second: Imm32) {
        default(dst, first, second)
    }

    override fun ria(dst: GPRegister, first: Imm32, second: Address) {
        default(dst, first, second)
    }

    override fun rir(dst: GPRegister, first: Imm32, second: GPRegister) {
        default(dst, first, second)
    }

    override fun rra(dst: GPRegister, first: GPRegister, second: Address) {
        default(dst, first, second)
    }

    override fun rri(dst: GPRegister, first: GPRegister, second: Imm32) {
        default(dst, first, second)
    }

    override fun rrr(dst: GPRegister, first: GPRegister, second: GPRegister) {
        default(dst, first, second)
    }

    override fun arr(dst: Address, first: GPRegister, second: GPRegister) {
        default(dst, first, second)
    }

    override fun default(dst: Operand, first: Operand, second: Operand) {
        throw RuntimeException("Internal error: '${GetElementPtr.NAME}' dst=$dst, first=$first, second=$second")
    }
}