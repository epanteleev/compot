package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.types.*
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.CallConvention.xmmTemp1
import ir.platform.x64.codegen.MacroAssembler
import ir.platform.x64.codegen.visitors.GPOperandsVisitorBinaryOp
import ir.platform.x64.codegen.visitors.XmmOperandsVisitorBinaryOp


data class SubCodegen(val type: IntegerType, val asm: MacroAssembler): GPOperandsVisitorBinaryOp {
    private val size: Int = type.sizeOf()

    operator fun invoke(dst: Operand, first: Operand, second: Operand) {
        GPOperandsVisitorBinaryOp.apply(dst, first, second, this)
    }

    override fun rrr(dst: GPRegister, first: GPRegister, second: GPRegister) {
        when {
            (first == dst) -> asm.sub(size, second, dst)
            else -> {
                asm.copy(size, first, temp1)
                asm.sub(size, second, temp1)
                asm.copy(size, temp1, dst)
            }
        }
    }

    override fun arr(dst: Address, first: GPRegister, second: GPRegister) {
        asm.mov(size, first, dst)
        asm.sub(size, second, dst)
    }

    override fun rar(dst: GPRegister, first: Address, second: GPRegister) {
        asm.mov(size, first, dst)
        asm.sub(size, second, dst)
    }

    override fun rir(dst: GPRegister, first: Imm32, second: GPRegister) {
        if (dst == second) {
            TODO()
            asm.sub(size, first, dst)
            asm.neg(size, dst)
        } else {
            asm.copy(size, first, dst)
            asm.sub(size, second, dst)
        }
    }

    override fun rra(dst: GPRegister, first: GPRegister, second: Address) {
        when {
            (first == dst) -> asm.sub(size, second, dst)
            else -> {
                asm.copy(size, first, dst)
                asm.sub(size, second, dst)
            }
        }
    }

    override fun rri(dst: GPRegister, first: GPRegister, second: Imm32) {
        if (dst == first) {
            asm.sub(size, second, dst)
        } else {
            asm.copy(size, first, dst)
            asm.sub(size, second, dst)
        }
    }

    override fun raa(dst: GPRegister, first: Address, second: Address) {
        asm.mov(size, first, dst)
        asm.sub(size, second, dst)
    }

    override fun rii(dst: GPRegister, first: Imm32, second: Imm32) {
        asm.copy(size, Imm32.of(first.value() - second.value()), dst) //TODO overflow
    }

    override fun ria(dst: GPRegister, first: Imm32, second: Address) {
        asm.copy(size, first, dst)
        asm.sub(size, second, dst)
    }

    override fun rai(dst: GPRegister, first: Address, second: Imm32) {
        asm.mov(size, first, dst)
        asm.sub(size, second, dst)
    }

    override fun ara(dst: Address, first: GPRegister, second: Address) {
        asm.copy(size, first, temp1)
        asm.sub(size, second, temp1)
        asm.mov(size, temp1, dst)
    }

    override fun aii(dst: Address, first: Imm32, second: Imm32) {
        asm.mov(size, Imm32.of(first.value() - second.value()), dst)
    }

    override fun air(dst: Address, first: Imm32, second: GPRegister) {
        TODO("Not yet implemented")
    }

    override fun aia(dst: Address, first: Imm32, second: Address) {
        TODO("Not yet implemented")
    }

    override fun ari(dst: Address, first: GPRegister, second: Imm32) {
        asm.mov(size, first, dst)
        asm.sub(size, second, dst)
    }

    override fun aai(dst: Address, first: Address, second: Imm32) {
        if (dst == first) {
            asm.sub(size, second, dst)
        } else {
            asm.mov(size, first, temp1)
            asm.sub(size, second, temp1)
            asm.mov(size, temp1, dst)
        }
    }

    override fun aar(dst: Address, first: Address, second: GPRegister) {
        if (dst == first) {
            asm.sub(size, second, dst)
        } else {
            asm.mov(size, first, temp1)
            asm.sub(size, second, temp1)
            asm.mov(size, temp1, dst)
        }
    }

    override fun aaa(dst: Address, first: Address, second: Address) {
        asm.mov(size, first, temp1)
        asm.sub(size, second, temp1)
        asm.mov(size, temp1, dst)
    }

    override fun default(dst: Operand, first: Operand, second: Operand) {
        throw RuntimeException("Internal error: '${ir.instruction.Sub.NAME}' dst=$dst, first=$first, second=$second")
    }
}