package ir.platform.x64.codegen.impl

import asm.x64.Operand
import asm.x64.*
import ir.Definitions.POINTER_SIZE
import ir.types.*
import ir.instruction.GetElementPtr
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.codegen.X64MacroAssembler
import ir.platform.x64.codegen.visitors.GPOperandsVisitorArithmeticBinaryOp


internal class GetElementPtrCodegen(val type: PtrType, private val secondOpSize: Int, basicType: NonTrivialType, val asm: X64MacroAssembler) :
    GPOperandsVisitorArithmeticBinaryOp {
    private val size: Int = basicType.sizeOf()

    operator fun invoke(dst: Operand, source: Operand, index: Operand) {
        GPOperandsVisitorArithmeticBinaryOp.apply(dst, source, index, this)
    }

    override fun rrr(dst: GPRegister, first: GPRegister, second: GPRegister) {
        if (ScaleFactor.isScaleFactor(size)) {
            asm.lea(POINTER_SIZE, Address.from(first, 0, second, ScaleFactor.from(size)), dst)
        } else {
            if (dst == first) {
                asm.copy(POINTER_SIZE, first, temp1)
                asm.imul(POINTER_SIZE, Imm32.of(size), second, dst)
                asm.add(POINTER_SIZE, temp1, dst)
            } else {
                asm.imul(POINTER_SIZE, Imm32.of(size), second, dst)
                asm.add(POINTER_SIZE, first, dst)
            }
        }
    }

    override fun arr(dst: Address, first: GPRegister, second: GPRegister) {
        if (ScaleFactor.isScaleFactor(size)) {
            asm.lea(POINTER_SIZE, Address.from(first, 0, second, ScaleFactor.from(size)), temp1)
            asm.mov(POINTER_SIZE, temp1, dst)
        } else {
            asm.imul(POINTER_SIZE, Imm32.of(size), second, temp1)
            asm.add(POINTER_SIZE, first, temp1)
            asm.mov(POINTER_SIZE, temp1, dst)
        }
    }

    override fun rar(dst: GPRegister, first: Address, second: GPRegister) {
        if (ScaleFactor.isScaleFactor(size)) {
            asm.mov(POINTER_SIZE, first, temp1)
            asm.lea(POINTER_SIZE, Address.from(temp1, 0, second, ScaleFactor.from(size)), dst)
        } else {
            asm.imul(POINTER_SIZE, Imm32.of(size), second, dst)
            asm.add(POINTER_SIZE, first, dst)
        }
    }

    override fun rir(dst: GPRegister, first: Imm32, second: GPRegister) {
        TODO("Not yet implemented")
    }

    override fun rra(dst: GPRegister, first: GPRegister, second: Address) {
        if (ScaleFactor.isScaleFactor(size)) {
            asm.mov(secondOpSize, second, temp1)
            asm.lea(POINTER_SIZE, Address.from(first, 0, temp1, ScaleFactor.from(size)), dst)
        } else {
            if (dst == first) {
                asm.copy(POINTER_SIZE, first, temp1)
                asm.imul(POINTER_SIZE, Imm32.of(size), second, dst)
                asm.add(POINTER_SIZE, temp1, dst)
            } else {
                asm.imul(POINTER_SIZE, Imm32.of(size), second, dst)
                asm.add(POINTER_SIZE, first, dst)
            }
        }
    }

    override fun rri(dst: GPRegister, first: GPRegister, second: Imm32) {
        val disp = second.value() * size
        asm.lea(POINTER_SIZE, Address.from(first, disp.toInt()), dst)
    }

    override fun raa(dst: GPRegister, first: Address, second: Address) {
        asm.mov(secondOpSize, second, temp1)
        asm.mov(POINTER_SIZE, first, dst)
        asm.lea(POINTER_SIZE, Address.from(dst, 0, temp1, ScaleFactor.from(size)), dst)
    }

    override fun rii(dst: GPRegister, first: Imm32, second: Imm32) {
        TODO("Not yet implemented")
    }

    override fun ria(dst: GPRegister, first: Imm32, second: Address) {
        TODO("Not yet implemented")
    }

    override fun rai(dst: GPRegister, first: Address, second: Imm32) {
        val disp = second.value() * size
        asm.mov(POINTER_SIZE, first, dst)
        asm.lea(POINTER_SIZE, Address.from(dst, disp.toInt()), dst)
    }

    override fun ara(dst: Address, first: GPRegister, second: Address) {
        if (ScaleFactor.isScaleFactor(size)) {
            asm.mov(secondOpSize, second, temp1)
            asm.lea(POINTER_SIZE, Address.from(first, 0, temp1, ScaleFactor.from(size)), temp1)
            asm.mov(POINTER_SIZE, temp1, dst)
        } else {
            asm.imul(POINTER_SIZE, Imm32.of(size), second, temp1)
            asm.add(POINTER_SIZE, first, temp1)
            asm.mov(POINTER_SIZE, temp1, dst)
        }
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
        val disp = second.value() * size
        asm.lea(POINTER_SIZE, Address.from(first, disp.toInt()), temp1)
        asm.mov(POINTER_SIZE, temp1, dst)
    }

    override fun aai(dst: Address, first: Address, second: Imm32) {
        val disp = second.value() * size
        asm.mov(POINTER_SIZE, first, temp1)
        asm.lea(POINTER_SIZE, Address.from(temp1, disp.toInt()), temp1)
        asm.mov(POINTER_SIZE, temp1, dst)
    }

    override fun aar(dst: Address, first: Address, second: GPRegister) {
        asm.imul(POINTER_SIZE, Imm32.of(size), second, temp1)
        asm.add(POINTER_SIZE, first, temp1)
        asm.mov(POINTER_SIZE, temp1, dst)
    }

    override fun aaa(dst: Address, first: Address, second: Address) {
        asm.imul(POINTER_SIZE, Imm32.of(size), second, temp1)
        asm.add(POINTER_SIZE, first, temp1)
        asm.mov(POINTER_SIZE, temp1, dst)
    }

    override fun default(dst: Operand, first: Operand, second: Operand) {
        throw RuntimeException("Internal error: '${GetElementPtr.NAME}' dst=$dst, first=$first, second=$second")
    }
}