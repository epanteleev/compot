package ir.platform.x64.codegen.impl

import asm.x64.Operand
import asm.x64.*
import ir.types.PrimitiveType
import ir.instruction.lir.StoreOnStack
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.CallConvention.temp2
import ir.platform.x64.CallConvention.xmmTemp1
import ir.platform.x64.codegen.visitors.GPOperandsVisitorArithmeticBinaryOp
import ir.types.FloatingPointType
import ir.types.IntegerType
import ir.types.PtrType


internal class StoreOnStackCodegen (val type: PrimitiveType, val indexType: IntegerType, val asm: Assembler) : GPOperandsVisitorArithmeticBinaryOp {
    private val size = type.sizeOf()
    private val indexSize = indexType.sizeOf()

    operator fun invoke(dst: Operand, source: Operand, index: Operand) {
        when (type) {
            is FloatingPointType -> {
                when {
                    dst is Address2 && source is XmmRegister && index is GPRegister -> {
                        asm.movf(size, source, Address.from(dst.base, dst.offset, index, ScaleFactor.from(size)))
                    }
                    dst is Address2 && source is XmmRegister && index is Imm -> {
                        asm.movf(size, source, Address.from(dst.base, dst.offset + index.asImm32().value().toInt() * size))
                    }
                    dst is Address2 && source is Address && index is Imm -> {
                        asm.movf(indexSize, source, xmmTemp1)
                        asm.movf(size, xmmTemp1, Address.from(dst.base, dst.offset + index.asImm32().value().toInt() * size))
                    }
                    else -> {
                        println("${index::class}")
                        default(dst, source, index)
                    }
                }
            }
            is IntegerType, is PtrType -> GPOperandsVisitorArithmeticBinaryOp.apply(dst, source, index, this)
            else -> throw RuntimeException("Unknown type=$type, dst=$dst, source=$source, index=$index")
        }
    }

    override fun rrr(dst: GPRegister, first: GPRegister, second: GPRegister) {
        TODO("Not yet implemented")
    }

    override fun arr(dst: Address, first: GPRegister, second: GPRegister) {
        when (dst) {
            is Address2 -> {
                asm.mov(size, first, Address.from(dst.base, dst.offset, second, ScaleFactor.from(size)))
            }
            else -> throw RuntimeException("Unknown type=$type, dst=$dst, first=$first, second=$second")
        }
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

    override fun ara(dst: Address, first: GPRegister, second: Address) = when (dst) {
        is Address2 -> {
            asm.mov(indexSize, second, temp1)
            asm.mov(size, first, Address.from(dst.base, dst.offset, temp1, ScaleFactor.from(size)))
        }
        else -> throw RuntimeException("Unknown type=$type, dst=$dst, first=$first, second=$second")
    }

    override fun aii(dst: Address, first: Imm32, second: Imm32) = when (dst) {
        is Address2 -> asm.mov(size, first, dst.withOffset(second.value().toInt() * size))
        else -> throw RuntimeException("Unknown type=$type, dst=$dst, first=$first, second=$second")
    }

    override fun air(dst: Address, first: Imm32, second: GPRegister) = when (dst) {
        is Address2 -> asm.mov(size, first, Address.from(dst.base, dst.offset, second, ScaleFactor.from(size)))
        else -> throw RuntimeException("Unknown type=$type, dst=$dst, first=$first, second=$second")
    }

    override fun aia(dst: Address, first: Imm32, second: Address) = when (dst) {
        is Address2 -> {
            asm.mov(indexSize, second, temp1)
            asm.mov(size, first, Address.from(dst.base, dst.offset, temp1, ScaleFactor.from(size)))
        }
        else -> throw RuntimeException("Unknown type=$type, dst=$dst, first=$first, second=$second")
    }

    override fun ari(dst: Address, first: GPRegister, second: Imm32) = when (dst) {
        is Address2 -> asm.mov(size, first, Address.from(dst.base, dst.offset + second.value().toInt() * size))
        else -> throw RuntimeException("Unknown type=$type, dst=$dst, first=$first, second=$second")
    }

    override fun aai(dst: Address, first: Address, second: Imm32) = when (dst) {
        is Address2 -> {
            asm.mov(size, first, temp1)
            asm.mov(size, temp1, Address.from(dst.base, dst.offset + second.value().toInt() * size))
        }
        else -> throw RuntimeException("Unknown type=$type, dst=$dst, first=$first, second=$second")
    }

    override fun aar(dst: Address, first: Address, second: GPRegister) {
        TODO("Not yet implemented")
    }

    override fun aaa(dst: Address, first: Address, second: Address) = when (dst) {
        is Address2 -> {
            asm.mov(indexSize, second, temp1)
            asm.mov(size, first, temp2)
            asm.mov(size, temp2, Address.from(dst.base, dst.offset, temp1, ScaleFactor.from(size)))
        }
        else -> throw RuntimeException("Unknown type=$type, dst=$dst, first=$first, second=$second")
    }

    override fun default(dst: Operand, first: Operand, second: Operand) {
        throw RuntimeException("Internal error: '${StoreOnStack.NAME}' dst=$dst, first=$first, second=$second")
    }
}