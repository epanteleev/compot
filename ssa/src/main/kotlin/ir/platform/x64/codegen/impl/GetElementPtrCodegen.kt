package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.types.*
import ir.instruction.GetElementPtr
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.CallConvention.isIntRange
import ir.platform.x64.CallConvention.POINTER_SIZE
import ir.platform.x64.codegen.utils.GPOperandVisitorBinaryOp


class GetElementPtrCodegen(val type: PointerType, private val secondOpSize: Int, basicType: PrimitiveType, val asm: Assembler) :
    GPOperandVisitorBinaryOp {
    private val size: Int = basicType.size()


    operator fun invoke(dst: Operand, source: Operand, index: Operand) {
        ir.platform.x64.codegen.utils.ApplyClosure(dst, source, index, this as GPOperandVisitorBinaryOp)
    }

    override fun rrr(dst: GPRegister, first: GPRegister, second: GPRegister) {
        asm.lea(POINTER_SIZE, Address.from(first, 0, second, size), dst)
    }

    override fun arr(dst: Address, first: GPRegister, second: GPRegister) {
        asm.lea(POINTER_SIZE, Address.from(first, 0, second, size), temp1)
        asm.mov(POINTER_SIZE, temp1, dst)
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
        val disp = second.value() * size
        assert(isIntRange(disp)) {
            "should be, but disp=$disp"
        }

        asm.lea(POINTER_SIZE, Address.from(first, disp.toInt()), dst)
    }

    override fun raa(dst: GPRegister, first: Address, second: Address) {
        TODO("Not yet implemented")
    }

    override fun rii(dst: GPRegister, first: Imm32, second: Imm32) {
        TODO("Not yet implemented")
    }

    override fun ria(dst: GPRegister, first: Imm32, second: Address) {
        TODO("Not yet implemented")
    }

    override fun rai(dst: GPRegister, first: Address, second: Imm32) {
        TODO("Not yet implemented")
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
        throw RuntimeException("Internal error: '${GetElementPtr.NAME}' dst=$dst, first=$first, second=$second")
    }
}