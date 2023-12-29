package ir.platform.x64.utils

import asm.x64.*
import ir.types.*
import ir.platform.x64.CodeEmitter
import ir.instruction.ArithmeticBinaryOp
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.CallConvention.xmmTemp1


data class AddCodegen(val type: PrimitiveType, val objFunc: ObjFunction): GPOperandVisitorBinaryOp, XmmOperandVisitor {
    private val size: Int = type.size()

    operator fun invoke(dst: AnyOperand, first: AnyOperand, second: AnyOperand) {
        when (type) {
            is FloatingPointType  -> ApplyClosureBinaryOp(dst, first, second, this as XmmOperandVisitor)
            is IntegerType    -> ApplyClosureBinaryOp(dst, first, second, this as GPOperandVisitorBinaryOp)
            else -> throw RuntimeException("Unknown type=$type, dst=$dst, first=$first, second=$second")
        }
    }

    override fun rrr(dst: GPRegister, first: GPRegister, second: GPRegister) {
        if (first == dst) {
            objFunc.add(size, second, dst)
        } else if (second == dst) {
            objFunc.add(size, first, dst)
        } else {
            objFunc.mov(size, first, dst)
            objFunc.add(size, second, dst)
        }
    }

    override fun arr(dst: Address, first: GPRegister, second: GPRegister) {
        objFunc.mov(size, first, dst)
        objFunc.add(size, second, dst)
    }

    override fun rar(dst: GPRegister, first: Address, second: GPRegister) {
        if (dst == second) {
            objFunc.add(size, first, second)
        } else {
            objFunc.mov(size, first, temp1)
            objFunc.add(size, second, temp1)
            objFunc.mov(size, temp1, dst)
        }
    }

    override fun rir(dst: GPRegister, first: ImmInt, second: GPRegister) {
        if (dst == second) {
            objFunc.add(size, first, dst)
        } else {
            objFunc.lea(size, Address.mem(second, first.value), dst)
        }
    }

    override fun rra(dst: GPRegister, first: GPRegister, second: Address) {
        objFunc.mov(size, first, dst)
        objFunc.add(size, second, dst)
    }

    override fun rri(dst: GPRegister, first: GPRegister, second: ImmInt) {
        if (dst == first) {
            objFunc.add(size, second, dst)
        } else {
            objFunc.lea(size, Address.mem(first, second.value), dst)
        }
    }

    override fun raa(dst: GPRegister, first: Address, second: Address) {
        TODO("Not yet implemented")
    }

    override fun rii(dst: GPRegister, first: ImmInt, second: ImmInt) {
        objFunc.mov(size, ImmInt(first.value + second.value), dst)
    }

    override fun ria(dst: GPRegister, first: ImmInt, second: Address) {
        TODO("Not yet implemented")
    }

    override fun rai(dst: GPRegister, first: Address, second: ImmInt) {
        TODO("Not yet implemented")
    }

    override fun ara(dst: Address, first: GPRegister, second: Address) {
        TODO("Not yet implemented")
    }

    override fun aii(dst: Address, first: ImmInt, second: ImmInt) {
        objFunc.mov(size, ImmInt(first.value + second.value), dst)
    }

    override fun air(dst: Address, first: ImmInt, second: GPRegister) {
        TODO("Not yet implemented")
    }

    override fun aia(dst: Address, first: ImmInt, second: Address) {
        TODO("Not yet implemented")
    }

    override fun ari(dst: Address, first: Register, second: ImmInt) {
        TODO("Not yet implemented")
    }

    override fun aai(dst: Address, first: Address, second: ImmInt) {
        TODO("Not yet implemented")
    }

    override fun aar(dst: Address, first: Address, second: GPRegister) {
        objFunc.mov(size, first, temp1)
        objFunc.add(size, second, temp1)
        objFunc.mov(size, temp1, dst)
    }

    override fun aaa(dst: Address, first: Address, second: Address) {
        if (dst == first) {
            objFunc.mov(size, second, temp1)
            objFunc.add(size, temp1, dst)
        } else if (dst == second) {
            objFunc.mov(size, first, temp1)
            objFunc.add(size, temp1, dst)
        } else {
            objFunc.mov(size, first, CodeEmitter.temp1)
            objFunc.add(size, second, CodeEmitter.temp1)
            objFunc.mov(size, CodeEmitter.temp1, dst)
        }
    }

    override fun rrrF(dst: XmmRegister, first: XmmRegister, second: XmmRegister) {
        if (first == dst) {
            objFunc.addf(size, second, dst)
        } else if (second == dst) {
            objFunc.addf(size, first, dst)
        } else {
            objFunc.movf(size, first, dst)
            objFunc.addf(size, second, dst)
        }
    }

    override fun arrF(dst: Address, first: XmmRegister, second: XmmRegister) {
        objFunc.movf(size, first, xmmTemp1)
        objFunc.addf(size, second, xmmTemp1)
        objFunc.movf(size, xmmTemp1, dst)
    }

    override fun rarF(dst: XmmRegister, first: Address, second: XmmRegister) {
        if (second == dst) {
            objFunc.addf(size, first, dst)
        } else {
            objFunc.movf(size, first, xmmTemp1)
            objFunc.addf(size, second, xmmTemp1)
            objFunc.movf(size, xmmTemp1, dst)
        }
    }

    override fun rirF(dst: XmmRegister, first: ImmFp, second: XmmRegister) {
        objFunc.mov(size, first.bits(), temp1)
        objFunc.movd(size, temp1, xmmTemp1)

        if (dst == second) {
            objFunc.addf(size, xmmTemp1, dst)
        } else {
            objFunc.addf(size, second, xmmTemp1)
            objFunc.movf(size, xmmTemp1, dst)
        }
    }

    override fun rraF(dst: XmmRegister, first: XmmRegister, second: Address) {
        TODO("Not yet implemented")
    }

    override fun rriF(dst: XmmRegister, first: XmmRegister, second: ImmFp) {
        objFunc.mov(size, second.bits(), temp1)
        objFunc.movd(size, temp1, xmmTemp1)

        if (dst == first) {
            objFunc.addf(size, xmmTemp1, dst)
        } else {
            objFunc.addf(size, first, xmmTemp1)
            objFunc.movf(size, xmmTemp1, dst)
        }
    }

    override fun raaF(dst: XmmRegister, first: Address, second: Address) {
        TODO("Not yet implemented")
    }

    override fun riiF(dst: XmmRegister, first: ImmFp, second: ImmFp) {
        objFunc.mov(size, (first + second).bits(), temp1)
        objFunc.movd(size, temp1, dst)
    }

    override fun riaF(dst: XmmRegister, first: ImmFp, second: Address) {
        TODO("Not yet implemented")
    }

    override fun raiF(dst: XmmRegister, first: Address, second: ImmFp) {
        TODO("Not yet implemented")
    }

    override fun araF(dst: Address, first: XmmRegister, second: Address) {
        TODO("Not yet implemented")
    }

    override fun aiiF(dst: Address, first: ImmFp, second: ImmFp) {
        TODO("Not yet implemented")
    }

    override fun airF(dst: Address, first: ImmFp, second: XmmRegister) {
        TODO("Not yet implemented")
    }

    override fun aiaF(dst: Address, first: ImmFp, second: Address) {
        TODO("Not yet implemented")
    }

    override fun ariF(dst: Address, first: Register, second: ImmFp) {
        TODO("Not yet implemented")
    }

    override fun aaiF(dst: Address, first: Address, second: ImmFp) {
        TODO("Not yet implemented")
    }

    override fun aarF(dst: Address, first: Address, second: XmmRegister) {
        objFunc.movf(size, first, xmmTemp1)
        objFunc.addf(size, second, xmmTemp1)
        objFunc.movf(size, xmmTemp1, dst)
    }

    override fun aaaF(dst: Address, first: Address, second: Address) {
        objFunc.movf(size, first, xmmTemp1)
        objFunc.addf(size, second, xmmTemp1)
        objFunc.movf(size, xmmTemp1, dst)
    }

    override fun error(dst: AnyOperand, first: AnyOperand, second: AnyOperand) {
        throw RuntimeException("Unimplemented: '${ArithmeticBinaryOp.Add}' dst=$dst, first=$first, second=$second")
    }
}