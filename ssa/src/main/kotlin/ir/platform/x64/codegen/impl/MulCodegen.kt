package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.types.*
import ir.instruction.ArithmeticBinaryOp
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.CallConvention.xmmTemp1
import ir.platform.x64.codegen.utils.ApplyClosure
import ir.platform.x64.codegen.utils.GPOperandVisitorBinaryOp
import ir.platform.x64.codegen.utils.XmmOperandVisitorBinaryOp


data class MulCodegen(val type: PrimitiveType, val asm: Assembler): GPOperandVisitorBinaryOp,
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
        if (first == dst) {
            asm.mul(size, second, dst)
        } else if (second == dst) {
            asm.mul(size, first, dst)
        } else {
            asm.mov(size, first, dst)
            asm.mul(size, second, dst)
        }
    }

    override fun arr(dst: Address, first: GPRegister, second: GPRegister) {
        asm.mov(size, first, dst)
        asm.mul(size, second, dst)
    }

    override fun rar(dst: GPRegister, first: Address, second: GPRegister) {
        if (dst == second) {
            asm.add(size, first, second)
        } else {
            asm.mov(size, first, temp1)
            asm.mul(size, second, temp1)
            asm.mov(size, temp1, dst)
        }
    }

    override fun rir(dst: GPRegister, first: Imm32, second: GPRegister) {
        if (dst == second) {
            asm.mul(size, first, dst)
        } else {
            asm.lea(size, Address.from(null, 0, second, first.value().toInt()), dst)
        }
    }

    override fun rra(dst: GPRegister, first: GPRegister, second: Address) {
        TODO("Not yet implemented")
    }

    override fun rri(dst: GPRegister, first: GPRegister, second: Imm32) {
        if (dst == first) {
            asm.mul(size, second, dst)
        } else {
            asm.lea(size, Address.from(null, 0, first, second.value().toInt()), dst)
        }
    }

    override fun raa(dst: GPRegister, first: Address, second: Address) {
        TODO("Not yet implemented")
    }

    override fun rii(dst: GPRegister, first: Imm32, second: Imm32) {
        asm.mov(size, Imm32(first.value() * second.value()), dst) //TODO overflow???
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
        asm.mov(size, Imm32(first.value() * second.value()), dst) //TODO overflow??
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
        if (first == dst) {
            asm.mov(size, second, temp1)
            asm.mul(size, temp1, dst)
        } else if (second == dst) {
            asm.mov(size, first, temp1)
            asm.mul(size, temp1, dst)
        } else {
            asm.mov(size, first, temp1)
            asm.mul(size, second, temp1)
            asm.mov(size, temp1, dst)
        }
    }

    override fun rrrF(dst: XmmRegister, first: XmmRegister, second: XmmRegister) {
        if (first == dst) {
            asm.mulf(size, second, dst)
        } else if (second == dst) {
            asm.mulf(size, first, dst)
        } else {
            asm.movf(size, first, xmmTemp1)
            asm.mulf(size, second, xmmTemp1)
            asm.movf(size, xmmTemp1, dst)
        }
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
        throw RuntimeException("Internal error: '${ArithmeticBinaryOp.Mul}' dst=$dst, first=$first, second=$second")
    }
}