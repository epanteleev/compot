package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.instruction.Move
import ir.platform.x64.CallConvention.POINTER_SIZE
import ir.platform.x64.CallConvention.isIntRange
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.CallConvention.temp2
import ir.platform.x64.codegen.utils.ApplyClosure
import ir.platform.x64.codegen.utils.GPOperandsVisitorBinaryOp
import ir.types.PrimitiveType

class MoveByIndexCodegen(val size: Int, basicType: PrimitiveType, val asm: Assembler) :
    GPOperandsVisitorBinaryOp {

    operator fun invoke(dst: Operand, source: Operand, index: Operand) {
        ApplyClosure(dst, source, index, this as GPOperandsVisitorBinaryOp)
    }

    override fun rrr(dst: GPRegister, first: GPRegister, second: GPRegister) {
        //asm.mov(size, Address.from(first, 0, second, size), dst)
        TODO()
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

    override fun rri(dst: GPRegister, first: GPRegister, second: Imm32) {
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
    }

    override fun aii(dst: Address, first: Imm32, second: Imm32) {
        asm.mov(POINTER_SIZE, dst, temp1)
        asm.mov(size, first, temp2)
        asm.mov(size, temp2, Address.from(temp1, second.value().toInt() * size))
    }

    override fun air(dst: Address, first: Imm32, second: GPRegister) {
        TODO("Not yet implemented")
    }

    override fun aia(dst: Address, first: Imm32, second: Address) {
        TODO("Not yet implemented")
    }

    override fun ari(dst: Address, first: GPRegister, second: Imm32) {
        val disp = second.value() * size
        assert(isIntRange(disp)) {
            "should be, but disp=$disp"
        }

        asm.mov(size, Address.from(first, disp.toInt()), temp1)
        asm.mov(size, temp1, dst)
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
        throw RuntimeException("Internal error: '${Move.NAME}' dst=$dst, first=$first, second=$second")
    }
}