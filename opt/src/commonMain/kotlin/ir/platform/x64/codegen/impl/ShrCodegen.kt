package ir.platform.x64.codegen.impl

import asm.x64.Operand
import asm.x64.*
import ir.types.*
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.codegen.X64MacroAssembler
import ir.platform.x64.codegen.visitors.GPOperandsVisitorBinaryOp


internal class ShrCodegen (type: ArithmeticType, val asm: X64MacroAssembler): GPOperandsVisitorBinaryOp {
    private val size: Int = type.sizeOf()

    operator fun invoke(dst: Operand, first: Operand, second: Operand) {
        GPOperandsVisitorBinaryOp.apply(dst, first, second, this)
    }

    override fun rrr(dst: GPRegister, first: GPRegister, second: GPRegister) {
        if (first == dst) {
            asm.shr(size, second, dst)
        } else if (second == dst) {
            asm.copy(size, first, temp1)
            asm.shr(size, second, temp1)
            asm.copy(size, temp1, dst)
        } else {
            asm.copy(size, first, dst)
            asm.shr(size, second, dst)
        }
    }

    override fun arr(dst: Address, first: GPRegister, second: GPRegister) {
        asm.mov(size, first, dst)
        asm.shr(size, second, dst)
    }

    override fun rar(dst: GPRegister, first: Address, second: GPRegister) {
        if (dst == second) {
            asm.mov(size, first, temp1)
            asm.shr(size, second, temp1)
            asm.copy(size, temp1, dst)
        } else {
            asm.mov(size, first, dst)
            asm.shr(size, second, dst)
        }
    }

    override fun rir(dst: GPRegister, first: Imm, second: GPRegister) {
        if (dst == second) {
            asm.mov(size, first, temp1)
            asm.shr(size, second, temp1)
            asm.copy(size, temp1, dst)
        } else {
            asm.copy(size, first, dst)
            asm.shr(size, second, dst)
        }
    }

    override fun rra(dst: GPRegister, first: GPRegister, second: Address) {
        TODO("Not yet implemented")
    }

    override fun rri(dst: GPRegister, first: GPRegister, second: Imm) {
        asm.copy(size, first, dst)
        asm.shr(size, Imm8.round(second.value()), dst)
    }

    override fun raa(dst: GPRegister, first: Address, second: Address) {
        TODO("Not yet implemented")
    }

    override fun rii(dst: GPRegister, first: Imm, second: Imm) {
        val value = first.value() ushr second.value().toInt() // TODO signed shift???
        asm.copy(size, Imm64.of(value), dst)
    }

    override fun ria(dst: GPRegister, first: Imm, second: Address) {
        TODO("Not yet implemented")
    }

    override fun rai(dst: GPRegister, first: Address, second: Imm) {
        asm.mov(size, first, dst)
        asm.shr(size, Imm8.round(second.value()), dst)
    }

    override fun ara(dst: Address, first: GPRegister, second: Address) {
        TODO("Not yet implemented")
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
        asm.mov(size, first, dst)
        asm.shr(size, Imm8.round(second.value()), dst)
    }

    override fun aai(dst: Address, first: Address, second: Imm) {
        if (dst == first) {
            asm.shr(size, Imm8.round(second.value()), dst)
        } else {
            asm.mov(size, first, temp1)
            asm.shr(size, Imm8.round(second.value()), temp1)
            asm.mov(size, temp1, dst)
        }
    }

    override fun aar(dst: Address, first: Address, second: GPRegister) {
        if (dst == first) {
            asm.shr(size, second, dst)
        } else {
            asm.mov(size, first, temp1)
            asm.shr(size, second, temp1)
            asm.mov(size, temp1, dst)
        }
    }

    override fun aaa(dst: Address, first: Address, second: Address) {
        TODO("Not yet implemented")
    }

    override fun default(dst: Operand, first: Operand, second: Operand) {
        throw RuntimeException("Internal error: '${ir.instruction.Shr.NAME}' dst=$dst, first=$first, second=$second")
    }
}