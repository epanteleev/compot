package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.types.PrimitiveType
import ir.instruction.lir.LoadFromStack
import ir.platform.x64.codegen.utils.ApplyClosure
import ir.platform.x64.codegen.utils.GPOperandsVisitorBinaryOp
import ir.types.FloatingPointType
import ir.types.IntegerType


class LoadFromStackCodegen (val type: PrimitiveType, val asm: Assembler) : GPOperandsVisitorBinaryOp {
    private val size = type.size()

    operator fun invoke(dst: Operand, source: Operand, index: Operand) {
        when (type) {
            is FloatingPointType -> default(dst, source, index)
            is IntegerType -> ApplyClosure(dst, source, index, this as GPOperandsVisitorBinaryOp)
            else -> throw RuntimeException("Unknown type=$type, dst=$dst, source=$source, index=$index")
        }
    }

    override fun rrr(dst: GPRegister, first: GPRegister, second: GPRegister) {
        TODO("Not yet implemented")
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
        when (first) {
            is Address2 -> {
                asm.mov(size, Address.from(first.base, first.offset + second.value().toInt() * size), dst)
            }
            else -> throw RuntimeException("Unknown type=$type, dst=$dst, first=$first, second=$second")
        }
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
        TODO("Not yet implemented")
    }

    override fun default(dst: Operand, first: Operand, second: Operand) {
        throw RuntimeException("Internal error: '${LoadFromStack.NAME}' dst=$dst, first=$first, second=$second")
    }
}