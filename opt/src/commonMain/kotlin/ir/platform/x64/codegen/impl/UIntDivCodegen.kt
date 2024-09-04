package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.types.*
import asm.x64.GPRegister.*
import common.assertion
import ir.Definitions.POINTER_SIZE
import ir.Definitions.WORD_SIZE
import ir.platform.x64.codegen.MacroAssembler
import ir.platform.x64.codegen.visitors.GPOperandsVisitorBinaryOp


class UIntDivCodegen(val type: ArithmeticType, val rem: Operand, val asm: MacroAssembler):
    GPOperandsVisitorBinaryOp {
    private val size: Int = type.sizeOf()

    operator fun invoke(dst: Operand, first: Operand, second: Operand) {
        assertion(second != rdx) { "Second operand cannot be rdx" }
        GPOperandsVisitorBinaryOp.apply(dst, first, second, this)
    }

    private fun prepareRegs(reg: GPRegister) {
        if (size != 1) {
            asm.xor(POINTER_SIZE, rdx, rdx)
            asm.mov(size, reg, rax)
        } else {
            asm.movzext(size, WORD_SIZE, reg, rax)
        }
    }

    private fun prepareAddress(reg: Address) {
        if (size != 1) {
            asm.xor(POINTER_SIZE, rdx, rdx)
            asm.mov(size, reg, rax)
        } else {
            asm.movzext(size, WORD_SIZE, reg, rax)
        }
    }

    override fun rrr(dst: GPRegister, first: GPRegister, second: GPRegister) {
        prepareRegs(first)
        asm.div(size, second)
        asm.mov(size, rax, dst)
        asm.moveRem(size, rem)
    }

    override fun arr(dst: Address, first: GPRegister, second: GPRegister) {
        prepareRegs(first)
        asm.div(size, second)
        asm.mov(size, rax, dst)
        asm.moveRem(size, rem)
    }

    override fun rar(dst: GPRegister, first: Address, second: GPRegister) {
        prepareAddress(first)
        asm.div(size, second)
        asm.mov(size, rax, dst)
        asm.moveRem(size, rem)
    }

    override fun rir(dst: GPRegister, first: Imm32, second: GPRegister) {
        TODO("Not yet implemented")
    }

    override fun rra(dst: GPRegister, first: GPRegister, second: Address) {
        prepareRegs(first)
        asm.div(size, second)
        asm.mov(size, rax, dst)
        asm.moveRem(size, rem)
    }

    override fun rri(dst: GPRegister, first: GPRegister, second: Imm32) {
        TODO("Not yet implemented")
    }

    override fun raa(dst: GPRegister, first: Address, second: Address) {
        prepareAddress(first)
        asm.div(size, second)
        asm.mov(size, rax, dst)
        asm.moveRem(size, rem)
    }

    override fun rii(dst: GPRegister, first: Imm32, second: Imm32) {
        TODO("untested")
        val imm = first.value() / second.value()
        asm.mov(size, Imm32.of(imm), dst)
        val remImm = first.value() % second.value()
        if (rem == rdx) {
            return
        }
        when (rem) {
            is GPRegister -> asm.mov(size, Imm32.of(remImm), rdx)
            is Address    -> asm.mov(size, Imm32.of(remImm), rdx)
            else -> throw RuntimeException("rem=$rem")
        }
    }

    override fun ria(dst: GPRegister, first: Imm32, second: Address) {
        TODO("Not yet implemented")
    }

    override fun rai(dst: GPRegister, first: Address, second: Imm32) {
        TODO("Not yet implemented")
    }

    override fun ara(dst: Address, first: GPRegister, second: Address) {
        prepareRegs(first)
        asm.div(size, second)
        asm.mov(size, rax, dst)
        asm.moveRem(size, rem)
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
        prepareAddress(first)
        asm.div(size, second)
        asm.mov(size, rax, dst)
        asm.moveRem(size, rem)
    }

    override fun aaa(dst: Address, first: Address, second: Address) {
        prepareAddress(first)
        asm.div(size, second)
        asm.mov(size, rax, dst)
        asm.moveRem(size, rem)
    }

    override fun default(dst: Operand, first: Operand, second: Operand) {
        throw RuntimeException("Internal error: '${ir.instruction.Div.NAME}' dst=$dst, first=$first, second=$second")
    }
}