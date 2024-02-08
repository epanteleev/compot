package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.types.*
import ir.instruction.GetElementPtr
import ir.platform.x64.CallConvention.POINTER_SIZE
import ir.platform.x64.codegen.utils.GPOperandVisitorBinaryOp


class GetFieldPtrCodegen(val type: PointerType, val basicType: AggregateType, val asm: Assembler):
    GPOperandVisitorBinaryOp {

    operator fun invoke(dst: Operand, source: Operand, index: Operand) {
        ir.platform.x64.codegen.utils.ApplyClosure(dst, source, index, this as GPOperandVisitorBinaryOp)
    }

    override fun rrr(dst: GPRegister, first: GPRegister, second: GPRegister) = default(dst, first, second)

    override fun arr(dst: Address, first: GPRegister, second: GPRegister) = default(dst, first, second)

    override fun rar(dst: GPRegister, first: Address, second: GPRegister) = default(dst, first, second)

    override fun rir(dst: GPRegister, first: Imm32, second: GPRegister) = default(dst, first, second)

    override fun rra(dst: GPRegister, first: GPRegister, second: Address) = default(dst, first, second)

    override fun rri(dst: GPRegister, first: GPRegister, second: Imm32) {
        val disp = basicType.offset(second.value().toInt())
        asm.lea(POINTER_SIZE, Address.from(first, disp), dst)
    }

    override fun raa(dst: GPRegister, first: Address, second: Address) = default(dst, first, second)

    override fun rii(dst: GPRegister, first: Imm32, second: Imm32) {
        TODO("Not yet implemented")
    }

    override fun ria(dst: GPRegister, first: Imm32, second: Address) = default(dst, first, second)

    override fun rai(dst: GPRegister, first: Address, second: Imm32) {
        TODO("Not yet implemented")
    }

    override fun ara(dst: Address, first: GPRegister, second: Address) = default(dst, first, second)

    override fun aii(dst: Address, first: Imm32, second: Imm32) {
        TODO("Not yet implemented")
    }

    override fun air(dst: Address, first: Imm32, second: GPRegister) = default(dst, first, second)

    override fun aia(dst: Address, first: Imm32, second: Address) = default(dst, first, second)

    override fun ari(dst: Address, first: GPRegister, second: Imm32) {
        TODO("Not yet implemented")
    }

    override fun aai(dst: Address, first: Address, second: Imm32) {
        TODO("Not yet implemented")
    }

    override fun aar(dst: Address, first: Address, second: GPRegister) = default(dst, first, second)

    override fun aaa(dst: Address, first: Address, second: Address) = default(dst, first, second)

    override fun default(dst: Operand, first: Operand, second: Operand) {
        throw RuntimeException("Internal error: '${GetElementPtr.NAME}' dst=$dst, first=$first, second=$second")
    }
}