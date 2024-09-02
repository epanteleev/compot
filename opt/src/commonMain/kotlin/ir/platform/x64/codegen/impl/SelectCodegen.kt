package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.types.*
import ir.instruction.*
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.CallConvention.temp2
import ir.platform.x64.codegen.visitors.*


class SelectCodegen(val type: PrimitiveType, val condition: CompareInstruction, val asm: Assembler): GPOperandsVisitorBinaryOp {
    private val size: Int = type.sizeOf()

    operator fun invoke(dst: Operand, first: Operand, second: Operand) {
        when (type) {
            is IntegerType -> GPOperandsVisitorBinaryOp.apply(dst, first, second, this)
            else -> throw RuntimeException("Unknown type=$type, dst=$dst, first=$first, second=$second")
        }
    }

    private fun matchIntCondition(): CMoveFlag {
        return when (condition.operandsType()) {
            is SignedIntType -> {
                when (condition.predicate()) {
                    IntPredicate.Eq -> CMoveFlag.CMOVE
                    IntPredicate.Ne -> CMoveFlag.CMOVNE
                    IntPredicate.Gt -> CMoveFlag.CMOVG
                    IntPredicate.Ge -> CMoveFlag.CMOVGE
                    IntPredicate.Lt -> CMoveFlag.CMOVL
                    IntPredicate.Le -> CMoveFlag.CMOVLE
                    else -> throw RuntimeException("unexpected condition type: condition=$condition")
                }
            }
            is UnsignedIntType, PointerType -> {
                when (condition.predicate()) {
                    IntPredicate.Eq -> CMoveFlag.CMOVE
                    IntPredicate.Ne -> CMoveFlag.CMOVNE
                    IntPredicate.Gt -> CMoveFlag.CMOVA
                    IntPredicate.Ge -> CMoveFlag.CMOVAE
                    IntPredicate.Lt -> CMoveFlag.CMOVB
                    IntPredicate.Le -> CMoveFlag.CMOVBE
                    else -> throw RuntimeException("unexpected condition type: condition=$condition")
                }
            }
            else -> throw RuntimeException("unexpected condition type: condition=$condition")
        }
    }

    override fun rrr(dst: GPRegister, first: GPRegister, second: GPRegister) {
        asm.mov(size, second, dst)
        asm.cmovcc(size, matchIntCondition(), first, dst)
    }

    override fun arr(dst: Address, first: GPRegister, second: GPRegister) {
        TODO("Not yet implemented")
    }

    override fun rar(dst: GPRegister, first: Address, second: GPRegister) {
        TODO("Not yet implemented")
    }

    override fun rir(dst: GPRegister, first: Imm32, second: GPRegister) {
        TODO("untested")
        asm.mov(size, second, dst)
        asm.mov(size, first, temp1)
        asm.cmovcc(size, matchIntCondition(), temp1, dst)
    }

    override fun rra(dst: GPRegister, first: GPRegister, second: Address) {
        TODO("Not yet implemented")
    }

    override fun rri(dst: GPRegister, first: GPRegister, second: Imm32) {
        asm.mov(size, second, dst)
        asm.cmovcc(size, matchIntCondition(), first, dst)
    }

    override fun raa(dst: GPRegister, first: Address, second: Address) {
        TODO("Not yet implemented")
    }

    override fun rii(dst: GPRegister, first: Imm32, second: Imm32) {
        if (first == second) {
            asm.mov(size, first, dst)
            return
        }
        asm.mov(size, second, dst)
        asm.mov(size, first, temp1)
        asm.cmovcc(size, matchIntCondition(), temp1, dst)
    }

    override fun ria(dst: GPRegister, first: Imm32, second: Address) {
        TODO("Not yet implemented")
    }

    override fun rai(dst: GPRegister, first: Address, second: Imm32) {
        asm.mov(size, second, dst)
        asm.cmovcc(size, matchIntCondition(), first, dst)
    }

    override fun ara(dst: Address, first: GPRegister, second: Address) {
        TODO("Not yet implemented")
    }

    override fun aii(dst: Address, first: Imm32, second: Imm32) {
        if (first == second) {
            asm.mov(size, first, dst)
            return
        }
        asm.mov(size, second, dst)
        asm.mov(size, first, temp1)
        asm.cmovcc(size, matchIntCondition(), temp1, temp2)
        asm.mov(size, temp2, dst)
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
        throw RuntimeException("Internal error: '${Select.NAME}' dst=$dst, first=$first, second=$second")
    }
}