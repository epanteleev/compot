package ir.platform.x64.codegen.impl

import asm.Operand
import asm.x64.*
import ir.types.*
import ir.instruction.*
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.CallConvention.temp2
import ir.platform.x64.codegen.X64MacroAssembler
import ir.platform.x64.codegen.visitors.*


internal class SelectCodegen(val type: IntegerType, val condition: IntCompare, val asm: X64MacroAssembler): GPOperandsVisitorBinaryOp {
    private val size: Int = type.sizeOf()

    operator fun invoke(dst: Operand, first: Operand, second: Operand) {
        GPOperandsVisitorBinaryOp.apply(dst, first, second, this)
    }

    private fun matchIntCondition(): CMoveFlag = asm.cMoveCondition(condition.predicate(), condition.operandsType)

    override fun rrr(dst: GPRegister, first: GPRegister, second: GPRegister) {
        if (first == second) {
            asm.copy(size, first, dst)
            return
        }
        when (dst) {
            first -> asm.cmovcc(size, matchIntCondition().invert(), second, dst)
            second -> asm.cmovcc(size, matchIntCondition(), first, dst)
            else -> {
                asm.copy(size, second, dst)
                asm.cmovcc(size, matchIntCondition(), first, dst)
            }
        }
    }

    override fun arr(dst: Address, first: GPRegister, second: GPRegister) {
        TODO("untested")
        if (first == second) {
            asm.mov(size, first, dst)
            return
        }
        asm.copy(size, second, temp1)
        asm.cmovcc(size, matchIntCondition(), first, temp1)
        asm.mov(size, temp1, dst)
    }

    override fun rar(dst: GPRegister, first: Address, second: GPRegister) {
        if (dst == second) {
            asm.cmovcc(size, matchIntCondition(), first, dst)
        } else {
            asm.copy(size, second, dst)
            asm.cmovcc(size, matchIntCondition(), first, dst)
        }
    }

    override fun rir(dst: GPRegister, first: Imm32, second: GPRegister) {
        TODO("untested")
        asm.copy(size, second, dst)
        asm.mov(size, first, temp1)
        asm.cmovcc(size, matchIntCondition(), temp1, dst)
    }

    override fun rra(dst: GPRegister, first: GPRegister, second: Address) {
        if (dst == first) {
            TODO("untested")
            asm.cmovcc(size, matchIntCondition(), second, dst)
        } else {
            asm.mov(size, second, dst)
            asm.cmovcc(size, matchIntCondition(), first, dst)
        }
    }

    override fun rri(dst: GPRegister, first: GPRegister, second: Imm32) {
        if (dst == first) {
            TODO("untested")
            asm.mov(size, second, temp1)
            asm.cmovcc(size, matchIntCondition().invert(), temp1, dst)
        } else {
            asm.mov(size, second, dst)
            asm.cmovcc(size, matchIntCondition(), first, dst)
        }
    }

    override fun raa(dst: GPRegister, first: Address, second: Address) {
        TODO("untested")
        if (first == second) {
            asm.mov(size, first, dst)
            return
        } else {
            asm.mov(size, second, dst)
            asm.cmovcc(size, matchIntCondition(), first, dst)
        }
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
        asm.mov(size, first, dst)
        asm.cmovcc(size, matchIntCondition().invert(), second, dst)
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
        asm.mov(size, second, temp2)
        asm.mov(size, first, temp1)
        asm.cmovcc(size, matchIntCondition(), temp1, temp2)
        asm.mov(size, temp2, dst)
    }

    override fun air(dst: Address, first: Imm32, second: GPRegister) {
        TODO("untested")
        asm.mov(size, first, temp1)
        asm.cmovcc(size, matchIntCondition().invert(), second, temp1)
        asm.mov(size, temp1, dst)
    }

    override fun aia(dst: Address, first: Imm32, second: Address) {
        TODO("untested")
        asm.mov(size, first, temp1)
        asm.cmovcc(size, matchIntCondition().invert(), second, temp1)
        asm.mov(size, temp1, dst)
    }

    override fun ari(dst: Address, first: GPRegister, second: Imm32) {
        asm.mov(size, second, temp1)
        asm.cmovcc(size, matchIntCondition(), first, temp1)
        asm.mov(size, temp1, dst)
    }

    override fun aai(dst: Address, first: Address, second: Imm32) {
        asm.mov(size, second, temp2)
        asm.mov(size, first, temp1)
        asm.cmovcc(size, matchIntCondition(), temp1, temp2)
        asm.mov(size, temp2, dst)
    }

    override fun aar(dst: Address, first: Address, second: GPRegister) {
        TODO("untested")
        asm.mov(size, first, temp1)
        asm.cmovcc(size, matchIntCondition().invert(), second, temp1)
        asm.mov(size, temp1, dst)
    }

    override fun aaa(dst: Address, first: Address, second: Address) {
        TODO("untested")
        if (first == second) {
            asm.mov(size, first, temp1)
            asm.mov(size, temp1, dst)
            return
        }
        when (dst) {
            second -> {
                asm.mov(size, first, temp1)
                asm.cmovcc(size, matchIntCondition().invert(), dst, temp1)
                asm.mov(size, temp1, dst)
            }
            second -> {
                asm.mov(size, second, temp2)
                asm.cmovcc(size, matchIntCondition(), first, temp2)
                asm.mov(size, temp2, dst)
            }
            else -> {
                asm.mov(size, second, temp2)
                asm.mov(size, first, temp1)
                asm.cmovcc(size, matchIntCondition(), temp1, temp2)
                asm.mov(size, temp2, dst)
            }
        }
    }

    override fun default(dst: Operand, first: Operand, second: Operand) {
        throw RuntimeException("Internal error: '${Select.NAME}' dst=$dst, first=$first, second=$second")
    }
}