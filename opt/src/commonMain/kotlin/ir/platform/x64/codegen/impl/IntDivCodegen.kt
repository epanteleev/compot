package ir.platform.x64.codegen.impl

import asm.x64.Operand
import asm.x64.*
import ir.types.*
import asm.x64.GPRegister.*
import ir.platform.x64.codegen.visitors.*
import ir.platform.x64.codegen.X64MacroAssembler


internal class IntDivCodegen(val type: ArithmeticType, val asm: X64MacroAssembler): GPOperandsVisitorArithmeticBinaryOp {
    private val size: Int = type.sizeOf()

    operator fun invoke(dst: Operand, first: Operand, second: Operand) {
        GPOperandsVisitorArithmeticBinaryOp.apply(dst, first, second, this)
    }

    override fun rrr(dst: GPRegister, first: GPRegister, second: GPRegister) {
        asm.copy(size, first, rax)
        asm.cdq(size)
        asm.idiv(size, second)
        asm.copy(size, rax, dst)
    }

    override fun arr(dst: Address, first: GPRegister, second: GPRegister) {
        asm.copy(size, first, rax)
        asm.cdq(size)
        asm.idiv(size, second)
        asm.mov(size, rax, dst)
    }

    override fun rar(dst: GPRegister, first: Address, second: GPRegister) {
        asm.mov(size, first, rax)
        asm.cdq(size)
        asm.idiv(size, second)
        asm.copy(size, rax, dst)
    }

    override fun rir(dst: GPRegister, first: Imm32, second: GPRegister) {
        asm.copy(size, first, rax)
        asm.cdq(size)
        asm.idiv(size, second)
        asm.copy(size, rax, dst)
    }

    override fun rra(dst: GPRegister, first: GPRegister, second: Address) {
        asm.copy(size, first, rax)
        asm.cdq(size)
        asm.idiv(size, second)
        asm.copy(size, rax, dst)
    }

    override fun rri(dst: GPRegister, first: GPRegister, second: Imm32) {
        asm.copy(size, first, rax)
        asm.copy(size, second, dst)
        asm.cdq(size)
        asm.idiv(size, dst)
        asm.copy(size, rax, dst)
    }

    override fun raa(dst: GPRegister, first: Address, second: Address) {
        asm.mov(size, first, rax)
        asm.cdq(size)
        asm.idiv(size, second)
        asm.copy(size, rax, dst)
    }

    override fun rii(dst: GPRegister, first: Imm32, second: Imm32) {
        val imm = first.value() / second.value()
        asm.copy(size, Imm32.of(imm), dst)
        val remImm = first.value() % second.value()
        asm.copy(size, Imm32.of(remImm), rdx)
    }

    override fun ria(dst: GPRegister, first: Imm32, second: Address) {
        TODO("Not yet implemented")
    }

    override fun rai(dst: GPRegister, first: Address, second: Imm32) {
        asm.mov(size, first, rax)
        asm.copy(size, second, dst)
        asm.cdq(size)
        asm.idiv(size, dst)
        asm.copy(size, rax, dst)
    }

    override fun ara(dst: Address, first: GPRegister, second: Address) {
        asm.copy(size, first, rax)
        asm.cdq(size)
        asm.idiv(size, second)
        asm.mov(size, rax, dst)
    }

    override fun aii(dst: Address, first: Imm32, second: Imm32) {
        val imm = first.value() / second.value()
        asm.mov(size, Imm32.of(imm), dst)
        val remImm = first.value() % second.value()
        asm.copy(size, Imm32.of(remImm), rdx)
    }

    override fun air(dst: Address, first: Imm32, second: GPRegister) {
        TODO("Not yet implemented")
    }

    override fun aia(dst: Address, first: Imm32, second: Address) {
        TODO("Not yet implemented")
    }

    override fun ari(dst: Address, first: GPRegister, second: Imm32) {
        asm.copy(size, first, rax)
        asm.mov(size, second, dst)
        asm.cdq(size)
        asm.idiv(size, dst)
        asm.mov(size, rax, dst)
    }

    override fun aai(dst: Address, first: Address, second: Imm32) {
        asm.mov(size, first, rax)
        asm.mov(size, second, dst)
        asm.cdq(size)
        asm.idiv(size, dst)
        asm.mov(size, rax, dst)
    }

    override fun aar(dst: Address, first: Address, second: GPRegister) {
        asm.mov(size, first, rax)
        asm.cdq(size)
        asm.idiv(size, second)
        asm.mov(size, rax, dst)
    }

    override fun aaa(dst: Address, first: Address, second: Address) {
        asm.mov(size, first, rax)
        asm.cdq(size)
        asm.idiv(size, second)
        asm.mov(size, rax, dst)
    }

    override fun default(dst: Operand, first: Operand, second: Operand) {
        throw RuntimeException("Internal error: '${ir.instruction.Div}' dst=$dst, first=$first, second=$second")
    }
}