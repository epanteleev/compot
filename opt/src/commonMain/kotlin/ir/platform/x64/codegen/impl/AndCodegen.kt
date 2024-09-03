package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.types.*
import ir.platform.x64.codegen.visitors.*
import ir.platform.x64.CallConvention.temp1


class AndCodegen(val type: ArithmeticType, val asm: Assembler): GPOperandsVisitorBinaryOp,
    XmmOperandsVisitorBinaryOp {
    private val size: Int = type.sizeOf()

    operator fun invoke(dst: Operand, first: Operand, second: Operand) = when (type) {
        is FloatingPointType -> XmmOperandsVisitorBinaryOp.apply(dst, first, second, this)
        is IntegerType       -> GPOperandsVisitorBinaryOp.apply(dst, first, second, this)
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
        if (dst == second) {
            asm.and(size, first, dst)
        } else {
            asm.mov(size, second, dst)
            asm.and(size, first, dst)
        }
    }

    override fun rra(dst: GPRegister, first: GPRegister, second: Address) {
        if (dst == first) {
            asm.and(size, second, dst)
        } else {
            asm.mov(size, first, dst)
            asm.and(size, second, dst)
        }
    }

    override fun rri(dst: GPRegister, first: GPRegister, second: Imm32) {
        if (dst == first) {
            asm.and(size, second, dst)
        } else {
            asm.mov(size, first, dst)
            asm.and(size, second, dst)
        }
    }

    override fun raa(dst: GPRegister, first: Address, second: Address) {
        TODO("Not yet implemented")
    }

    override fun rii(dst: GPRegister, first: Imm32, second: Imm32) {
        val and = first.value() and second.value()
        asm.mov(size, Imm32.of(and), dst)
    }

    override fun ria(dst: GPRegister, first: Imm32, second: Address) {
        TODO("Not yet implemented")
    }

    override fun rai(dst: GPRegister, first: Address, second: Imm32) {
        asm.mov(size, first, dst)
        asm.and(size, second, dst)
    }

    override fun ara(dst: Address, first: GPRegister, second: Address) {
        if (dst == second) {
            asm.and(size, first, dst)
        } else {
            asm.mov(size, first, temp1)
            asm.and(size, second, temp1)
            asm.mov(size, temp1, dst)
        }
    }

    override fun aii(dst: Address, first: Imm32, second: Imm32) {
        TODO("Not yet implemented")
    }

    override fun air(dst: Address, first: Imm32, second: GPRegister) {
        TODO("Not yet implemented")
    }

    override fun aia(dst: Address, first: Imm32, second: Address) {
        if (second == dst) {
            asm.and(size, first, dst)
        } else {
            asm.mov(size, first, temp1)
            asm.and(size, second, temp1)
            asm.mov(size, temp1, dst)
        }
    }

    override fun ari(dst: Address, first: GPRegister, second: Imm32) {
        asm.mov(size, first, dst)
        asm.and(size, second, dst)
    }

    override fun aai(dst: Address, first: Address, second: Imm32) {
        TODO("Not yet implemented")
    }

    override fun aar(dst: Address, first: Address, second: GPRegister) {
        TODO("Not yet implemented")
    }

    override fun aaa(dst: Address, first: Address, second: Address) {
        if (first == dst) {
            asm.and(size, second, temp1)
            asm.mov(size, temp1, dst)
        } else {
            asm.mov(size, first, temp1)
            asm.and(size, second, temp1)
            asm.mov(size, temp1, dst)
        }
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
        throw RuntimeException("Internal error: '${ir.instruction.And.NAME}' dst=$dst, first=$first, second=$second")
    }
}