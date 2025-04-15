package ir.platform.x64.codegen.impl

import asm.x64.Operand
import asm.x64.*
import ir.types.*
import common.assertion
import asm.x64.GPRegister.*
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.codegen.X64MacroAssembler
import ir.platform.x64.codegen.visitors.GPOperandsVisitorBinaryOp


internal class UIntDivCodegen(val type: ArithmeticType, val asm: X64MacroAssembler): GPOperandsVisitorBinaryOp {
    private val size: Int = type.sizeOf()

    operator fun invoke(dst: Operand, first: Operand, second: Operand) {
        assertion(second != rdx) { "Second operand cannot be rdx" }
        GPOperandsVisitorBinaryOp.apply(dst, first, second, this)
    }

    override fun rrr(dst: GPRegister, first: GPRegister, second: GPRegister) {
        asm.copy(size, first, rax)
        asm.xor(size, rdx, rdx)
        asm.div(size, second)
        asm.copy(size, rax, dst)
    }

    override fun arr(dst: Address, first: GPRegister, second: GPRegister) {
        asm.copy(size, first, rax)
        asm.xor(size, rdx, rdx)
        asm.div(size, second)
        asm.mov(size, rax, dst)
    }

    override fun rar(dst: GPRegister, first: Address, second: GPRegister) {
        asm.mov(size, first, rax)
        asm.xor(size, rdx, rdx)
        asm.div(size, second)
        asm.copy(size, rax, dst)
    }

    override fun rir(dst: GPRegister, first: Imm, second: GPRegister) {
        asm.copy(size, first, rax)
        asm.xor(size, rdx, rdx)
        asm.div(size, second)
        asm.copy(size, rax, dst)
    }

    override fun rra(dst: GPRegister, first: GPRegister, second: Address) {
        asm.copy(size, first, rax)
        asm.xor(size, rdx, rdx)
        asm.div(size, second)
        asm.copy(size, rax, dst)
    }

    override fun rri(dst: GPRegister, first: GPRegister, second: Imm) {
        asm.copy(size, first, rax)
        asm.copy(size, second, dst)
        asm.xor(size, rdx, rdx)
        asm.div(size, dst)
        asm.copy(size, rax, dst)
    }

    override fun raa(dst: GPRegister, first: Address, second: Address) {
        asm.mov(size, first, rax)
        asm.xor(size, rdx, rdx)
        asm.div(size, second)
        asm.copy(size, rax, dst)
    }

    override fun rii(dst: GPRegister, first: Imm, second: Imm) {
        val imm = first.value().toULong() / second.value().toULong()
        asm.copy(size, Imm64.of(imm.toLong()), dst)
        val remImm = first.value().toULong() % second.value().toULong()
        asm.copy(size, Imm64.of(remImm.toLong()), rdx)
    }

    override fun ria(dst: GPRegister, first: Imm, second: Address) {
        TODO("Not yet implemented")
    }

    override fun rai(dst: GPRegister, first: Address, second: Imm) {
        asm.mov(size, first, rax)
        asm.mov(size, second, dst)
        asm.xor(size, rdx, rdx)
        asm.div(size, dst)
        asm.copy(size, rax, dst)
    }

    override fun ara(dst: Address, first: GPRegister, second: Address) {
        asm.copy(size, first, rax)
        asm.xor(size, rdx, rdx)
        asm.div(size, second)
        asm.mov(size, rax, dst)
    }

    override fun aii(dst: Address, first: Imm, second: Imm) {
        TODO("Not yet implemented")
    }

    override fun air(dst: Address, first: Imm, second: GPRegister) {
        TODO("Not yet implemented")
    }

    override fun aia(dst: Address, first: Imm, second: Address) {
        TODO("Not yet implemented")
    }

    override fun ari(dst: Address, first: GPRegister, second: Imm) {
        if (Imm.canBeImm32(second.value())) {
            asm.mov(size, second.asImm32(), dst)
        } else {
            asm.copy(size, second, temp1)
            asm.mov(size, temp1, dst)
        }

        asm.copy(size, first, rax)
        asm.xor(size, rdx, rdx)
        asm.div(size, dst)
        asm.mov(size, rax, dst)
    }

    override fun aai(dst: Address, first: Address, second: Imm) {
        if (Imm.canBeImm32(second.value())) {
            asm.mov(size, second.asImm32(), dst)
        } else {
            asm.copy(size, second, temp1)
            asm.mov(size, temp1, dst)
        }

        asm.mov(size, first, rax)
        asm.xor(size, rdx, rdx)
        asm.div(size, dst)
        asm.mov(size, rax, dst)
    }

    override fun aar(dst: Address, first: Address, second: GPRegister) {
        asm.mov(size, first, rax)
        asm.xor(size, rdx, rdx)
        asm.div(size, second)
        asm.mov(size, rax, dst)
    }

    override fun aaa(dst: Address, first: Address, second: Address) {
        asm.mov(size, first, rax)
        asm.xor(size, rdx, rdx)
        asm.div(size, second)
        asm.mov(size, rax, dst)
    }

    override fun default(dst: Operand, first: Operand, second: Operand) {
        throw RuntimeException("Internal error: '${ir.instruction.Div.NAME}' dst=$dst, first=$first, second=$second")
    }
}