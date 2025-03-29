package ir.platform.x64.codegen.impl

import asm.Operand
import asm.x64.*
import ir.types.*
import ir.instruction.lir.LoadFromStack
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.codegen.visitors.GPOperandsVisitorBinaryOp


internal class LoadFromStackCodegen (val type: PrimitiveType, indexType: IntegerType, val asm: Assembler) : GPOperandsVisitorBinaryOp {
    private val size = type.sizeOf()
    private val indexSize = indexType.sizeOf()

    operator fun invoke(dst: Operand, source: Operand, index: Operand) {
        when (type) {
            is FloatingPointType -> {
                if (dst is XmmRegister && source is Address2 && index is ImmInt) {
                    asm.movf(size, Address.from(source.base, source.offset + index.value().toInt() * size), dst)
                } else if (dst is XmmRegister && source is Address2 && index is GPRegister) {
                    asm.movf(size, Address.from(source.base, source.offset, index, ScaleFactor.from(size)), dst)
                } else if (dst is XmmRegister && source is Address2 && index is Address2) {
                    asm.mov(indexSize, index, temp1)
                    asm.movf(size, Address.from(source.base, source.offset, temp1, ScaleFactor.from(size)), dst)
                } else {
                    default(dst, source, index)
                }
            }
            is IntegerType, is PtrType -> GPOperandsVisitorBinaryOp.apply(dst, source, index, this)
            else -> throw RuntimeException("Unknown type=$type, dst=$dst, source=$source, index=$index")
        }
    }

    override fun rrr(dst: GPRegister, first: GPRegister, second: GPRegister) = default(dst, first, second)

    override fun arr(dst: Address, first: GPRegister, second: GPRegister) = default(dst, first, second)

    override fun rar(dst: GPRegister, first: Address, second: GPRegister) {
        if (first is Address2) {
            asm.mov(size, Address.from(first.base, first.offset, second, ScaleFactor.from(size)), dst)
        } else {
            throw RuntimeException("Unknown type=$type, dst=$dst, first=$first, second=$second")
        }
    }

    override fun rir(dst: GPRegister, first: Imm32, second: GPRegister) = default(dst, first, second)

    override fun rra(dst: GPRegister, first: GPRegister, second: Address) = default(dst, first, second)

    override fun rri(dst: GPRegister, first: GPRegister, second: Imm32) = default(dst, first, second)

    override fun raa(dst: GPRegister, first: Address, second: Address) {
        if (first is Address2) {
            asm.mov(indexSize, second, temp1)
            asm.mov(size, Address.from(first.base, first.offset, temp1, ScaleFactor.from(size)), dst)
        } else {
            throw RuntimeException("Unknown type=$type, dst=$dst, first=$first, second=$second")
        }
    }

    override fun rii(dst: GPRegister, first: Imm32, second: Imm32) = default(dst, first, second)

    override fun ria(dst: GPRegister, first: Imm32, second: Address) = default(dst, first, second)

    override fun rai(dst: GPRegister, first: Address, second: Imm32) {
        when (first) {
            is Address2 -> {
                asm.mov(size, Address.from(first.base, first.offset + second.value().toInt() * size), dst)
            }
            else -> throw RuntimeException("Unknown type=$type, dst=$dst, first=$first, second=$second")
        }
    }

    override fun ara(dst: Address, first: GPRegister, second: Address) = default(dst, first, second)

    override fun aii(dst: Address, first: Imm32, second: Imm32) = default(dst, first, second)

    override fun air(dst: Address, first: Imm32, second: GPRegister) = default(dst, first, second)

    override fun aia(dst: Address, first: Imm32, second: Address) = default(dst, first, second)

    override fun ari(dst: Address, first: GPRegister, second: Imm32) = default(dst, first, second)

    override fun aai(dst: Address, first: Address, second: Imm32) {
        when (first) {
            is Address2 -> {
                asm.mov(size, Address.from(first.base, first.offset + second.value().toInt() * size), temp1)
                asm.mov(size, temp1, dst)
            }
            else -> throw RuntimeException("Unknown type=$type, dst=$dst, first=$first, second=$second")
        }
    }

    override fun aar(dst: Address, first: Address, second: GPRegister) {
        TODO("Not yet implemented")
    }

    override fun aaa(dst: Address, first: Address, second: Address) = when (first) {
        is Address2 -> {
            asm.mov(indexSize, second, temp1)
            asm.mov(size, Address.from(first.base, first.offset, temp1, ScaleFactor.from(size)), temp1)
            asm.mov(size, temp1, dst)
        }
        else -> throw RuntimeException("Unknown type=$type, dst=$dst, first=$first, second=$second")
    }

    override fun default(dst: Operand, first: Operand, second: Operand) {
        throw RuntimeException("Internal error: '${LoadFromStack.NAME}' dst=$dst, first=$first, second=$second")
    }
}