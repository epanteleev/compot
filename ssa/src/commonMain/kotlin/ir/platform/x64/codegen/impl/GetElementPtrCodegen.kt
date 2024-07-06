package ir.platform.x64.codegen.impl

import asm.x64.*
import common.assertion
import ir.types.*
import ir.instruction.GetElementPtr
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.CallConvention.isIntRange
import ir.platform.x64.CallConvention.POINTER_SIZE
import ir.platform.x64.codegen.visitors.GPOperandsVisitorBinaryOp


class GetElementPtrCodegen(val type: PointerType, private val secondOpSize: Int, basicType: NonTrivialType, val asm: Assembler) :
    GPOperandsVisitorBinaryOp {
    private val size: Int = basicType.sizeof()


    operator fun invoke(dst: Operand, source: Operand, index: Operand) {
        GPOperandsVisitorBinaryOp.apply(dst, source, index, this)
    }

    override fun rrr(dst: GPRegister, first: GPRegister, second: GPRegister) {
        asm.lea(POINTER_SIZE, Address.from(first, 0, second, size), dst)
    }

    override fun arr(dst: Address, first: GPRegister, second: GPRegister) {
        asm.lea(POINTER_SIZE, Address.from(first, 0, second, size), temp1)
        asm.mov(POINTER_SIZE, temp1, dst)
    }

    override fun rar(dst: GPRegister, first: Address, second: GPRegister) {
        asm.mov(POINTER_SIZE, first, temp1)
        asm.lea(POINTER_SIZE, Address.from(temp1, 0, second, size), dst)
    }

    override fun rir(dst: GPRegister, first: Imm32, second: GPRegister) {
        TODO("Not yet implemented")
    }

    override fun rra(dst: GPRegister, first: GPRegister, second: Address) {
        asm.mov(secondOpSize, second, temp1)
        asm.lea(POINTER_SIZE, Address.from(first, 0, temp1, size), temp1)
        asm.mov(POINTER_SIZE, temp1, dst)
    }

    override fun rri(dst: GPRegister, first: GPRegister, second: Imm32) {
        val disp = second.value() * size
        assertion(isIntRange(disp)) {
            "should be, but disp=$disp"
        }

        asm.lea(POINTER_SIZE, Address.from(first, disp.toInt()), dst)
    }

    override fun raa(dst: GPRegister, first: Address, second: Address) {
        asm.mov(secondOpSize, second, temp1)
        asm.mov(POINTER_SIZE, first, dst)
        asm.lea(POINTER_SIZE, Address.from(dst, 0, temp1, size), dst)
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
        asm.mov(secondOpSize, second, temp1)
        asm.lea(POINTER_SIZE, Address.from(first, 0, temp1, size), temp1)
        asm.mov(POINTER_SIZE, temp1, dst)
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
        TODO("Not yet implemented")
    }

    override fun aaa(dst: Address, first: Address, second: Address) {
        TODO("Not yet implemented")
    }

    override fun default(dst: Operand, first: Operand, second: Operand) {
        throw RuntimeException("Internal error: '${GetElementPtr.NAME}' dst=$dst, first=$first, second=$second")
    }
}