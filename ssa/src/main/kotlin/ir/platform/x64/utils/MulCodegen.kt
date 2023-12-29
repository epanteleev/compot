package ir.platform.x64.utils

import asm.x64.*
import ir.types.*
import ir.instruction.ArithmeticBinaryOp
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.CallConvention.xmmTemp1


data class MulCodegen(val type: PrimitiveType, val objFunc: ObjFunction): GPOperandVisitorBinaryOp, XmmOperandVisitor {
    private val size: Int = type.size()

    operator fun invoke(dst: AnyOperand, first: AnyOperand, second: AnyOperand) {
        when (type) {
            is FloatingPointType -> ApplyClosureBinaryOp(dst, first, second, this as XmmOperandVisitor)
            is IntegerType   -> ApplyClosureBinaryOp(dst, first, second, this as GPOperandVisitorBinaryOp)
            else -> throw RuntimeException("Unknown type=$type, dst=$dst, first=$first, second=$second")
        }
    }

    override fun rrr(dst: GPRegister, first: GPRegister, second: GPRegister) {
        if (first == dst) {
            objFunc.mul(size, second, dst)
        } else if (second == dst) {
            objFunc.mul(size, first, dst)
        } else {
            objFunc.mov(size, first, dst)
            objFunc.mul(size, second, dst)
        }
    }

    override fun arr(dst: Address, first: GPRegister, second: GPRegister) {
        objFunc.mov(size, first, dst)
        objFunc.mul(size, second, dst)
    }

    override fun rar(dst: GPRegister, first: Address, second: GPRegister) {
        if (dst == second) {
            objFunc.add(size, first, second)
        } else {
            objFunc.mov(size, first, temp1)
            objFunc.mul(size, second, temp1)
            objFunc.mov(size, temp1, dst)
        }
    }

    override fun rir(dst: GPRegister, first: Imm32, second: GPRegister) {
        if (dst == second) {
            objFunc.mul(size, first, dst)
        } else {
            objFunc.lea(size, Address.mem(null, 0, second, first.value), dst)
        }
    }

    override fun rra(dst: GPRegister, first: GPRegister, second: Address) {
        TODO("Not yet implemented")
    }

    override fun rri(dst: GPRegister, first: GPRegister, second: Imm32) {
        if (dst == first) {
            objFunc.mul(size, second, dst)
        } else {
            objFunc.lea(size, Address.mem(null, 0, first, second.value), dst)
        }
    }

    override fun raa(dst: GPRegister, first: Address, second: Address) {
        TODO("Not yet implemented")
    }

    override fun rii(dst: GPRegister, first: Imm32, second: Imm32) {
        objFunc.mov(size, Imm32(first.value * second.value), dst) //TODO overflow???
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
        objFunc.mov(size, Imm32(first.value * second.value), dst) //TODO overflow??
    }

    override fun air(dst: Address, first: Imm32, second: GPRegister) {
        TODO("Not yet implemented")
    }

    override fun aia(dst: Address, first: Imm32, second: Address) {
        TODO("Not yet implemented")
    }

    override fun ari(dst: Address, first: Register, second: Imm32) {
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
            objFunc.mov(size, second, temp1)
            objFunc.mul(size, temp1, dst)
        } else if (second == dst) {
            objFunc.mov(size, first, temp1)
            objFunc.mul(size, temp1, dst)
        } else {
            objFunc.mov(size, first, temp1)
            objFunc.mul(size, second, temp1)
            objFunc.mov(size, temp1, dst)
        }
    }

    override fun rrrF(dst: XmmRegister, first: XmmRegister, second: XmmRegister) {
        if (first == dst) {
            objFunc.mulf(size, second, dst)
        } else if (second == dst) {
            objFunc.mulf(size, first, dst)
        } else {
            objFunc.movf(size, first, xmmTemp1)
            objFunc.mulf(size, second, xmmTemp1)
            objFunc.movf(size, xmmTemp1, dst)
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

    override fun error(dst: AnyOperand, first: AnyOperand, second: AnyOperand) {
        throw RuntimeException("Unimplemented: '${ArithmeticBinaryOp.Mul}' dst=$dst, first=$first, second=$second")
    }
}