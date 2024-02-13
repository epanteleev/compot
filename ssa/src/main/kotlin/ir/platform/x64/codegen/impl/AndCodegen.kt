package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.types.*
import ir.platform.x64.codegen.utils.*
import ir.instruction.ArithmeticBinaryOp
import ir.platform.x64.CallConvention.temp1


class AndCodegen(val type: ArithmeticType, val asm: Assembler): GPOperandVisitorBinaryOp,
    XmmOperandVisitorBinaryOp {
    private val size: Int = type.size()

    operator fun invoke(dst: Operand, first: Operand, second: Operand) {
        when (type) {
            is FloatingPointType -> ApplyClosure(dst, first, second, this as XmmOperandVisitorBinaryOp)
            is IntegerType       -> ApplyClosure(dst, first, second, this as GPOperandVisitorBinaryOp)
            else -> throw RuntimeException("Unknown type=$type, dst=$dst, first=$first, second=$second")
        }
    }

    override fun rrr(dst: GPRegister, first: GPRegister, second: GPRegister) {
        when (dst) {
            first -> {
                asm.mov(size, second, temp1)
                asm.and(size, temp1, dst)
            }
            second -> {
                asm.mov(size, first, temp1)
                asm.and(size, temp1, dst)
            }
            else -> {
                asm.mov(size, first, temp1)
                asm.and(size, second, temp1)
                asm.mov(size, temp1, dst)
            }
        }
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

    override fun rrrF(dst: XmmRegister, first: XmmRegister, second: XmmRegister) {
        TODO("Not yet implemented")
    }

    override fun arrF(dst: Address, first: XmmRegister, second: XmmRegister) {
        TODO("Not yet implemented")
    }

    override fun rarF(dst: XmmRegister, first: Address, second: XmmRegister) {
        TODO("Not yet implemented")
    }

    override fun rraF(dst: XmmRegister, first: XmmRegister, second: Address) {
        TODO("Not yet implemented")
    }

    override fun raaF(dst: XmmRegister, first: Address, second: Address) {
        TODO("Not yet implemented")
    }

    override fun araF(dst: Address, first: XmmRegister, second: Address) {
        TODO("Not yet implemented")
    }

    override fun aarF(dst: Address, first: Address, second: XmmRegister) {
        TODO("Not yet implemented")
    }

    override fun aaaF(dst: Address, first: Address, second: Address) {
        TODO("Not yet implemented")
    }

    override fun default(dst: Operand, first: Operand, second: Operand) {
        throw RuntimeException("Internal error: '${ArithmeticBinaryOp.And}' dst=$dst, first=$first, second=$second")
    }
}