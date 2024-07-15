package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.types.*
import asm.x64.GPRegister.*
import ir.platform.x64.codegen.visitors.*
import ir.instruction.ArithmeticBinaryOp
import ir.platform.x64.codegen.MacroAssembler


data class IntDivCodegen(val type: ArithmeticType, val rem: Operand, val asm: MacroAssembler): GPOperandsVisitorBinaryOp {
    private val size: Int = type.sizeOf()

    operator fun invoke(dst: Operand, first: Operand, second: Operand) {
        when (type) {
            is IntegerType -> GPOperandsVisitorBinaryOp.apply(dst, first, second, this)
            else -> throw RuntimeException("Unknown type=$type, dst=$dst, first=$first, second=$second")
        }
    }

    override fun rrr(dst: GPRegister, first: GPRegister, second: GPRegister) {
        asm.mov(size, first, rax)
        asm.cdq(size)
        asm.idiv(size, second)
        asm.mov(size, rax, dst)
        asm.moveRem(size, rem)
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

    override fun rri(dst: GPRegister, first: GPRegister, second: Imm32) = default(dst, first, second)

    override fun raa(dst: GPRegister, first: Address, second: Address) {
        TODO("Not yet implemented")
    }

    override fun rii(dst: GPRegister, first: Imm32, second: Imm32) {
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
        asm.mov(size, first, rax)
        asm.cdq(size)
        asm.idiv(size, second)
        asm.mov(size, rax, dst)
        asm.moveRem(size, rem)
    }

    override fun default(dst: Operand, first: Operand, second: Operand) {
        throw RuntimeException("Internal error: '${ArithmeticBinaryOp.Div}' dst=$dst, first=$first, second=$second")
    }
}