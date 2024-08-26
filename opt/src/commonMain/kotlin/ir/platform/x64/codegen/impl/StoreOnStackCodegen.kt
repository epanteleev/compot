package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.types.PrimitiveType
import ir.instruction.lir.StoreOnStack
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.CallConvention.xmmTemp1
import ir.platform.x64.codegen.visitors.GPOperandsVisitorBinaryOp
import ir.types.FloatingPointType
import ir.types.IntegerType
import ir.types.PointerType


class StoreOnStackCodegen (val type: PrimitiveType, val asm: Assembler) : GPOperandsVisitorBinaryOp {
    private val size = type.sizeOf()

    operator fun invoke(dst: Operand, source: Operand, index: Operand) {
        when (type) {
            is FloatingPointType -> {
                when {
                    dst is Address2 && source is XmmRegister && index is GPRegister -> {
                        asm.movf(size, source, Address.from(dst.base, dst.offset, index, ScaleFactor.from(size)))
                    }
                    dst is Address2 && source is XmmRegister && index is Imm -> {
                        val indexImm = index as ImmInt
                        asm.movf(size, source, Address.from(dst.base, dst.offset + indexImm.asImm32().value().toInt() * size))
                    }
                    dst is Address && source is Address && index is Imm -> {
                        TODO("untested")
                        val indexImm = index as ImmInt
                        if (dst is Address2) {
                            asm.movf(size, source, xmmTemp1)
                            asm.movf(size, xmmTemp1, Address.from(dst.base, dst.offset + indexImm.asImm32().value().toInt() * size))
                        } else {
                            default(dst, source, indexImm)
                        }
                    }
                    else -> {
                        println("${index::class}")
                        default(dst, source, index)
                    }
                }
            }
            is IntegerType, is PointerType -> GPOperandsVisitorBinaryOp.apply(dst, source, index, this)
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

    override fun ara(dst: Address, first: GPRegister, second: Address) {
        TODO("Not yet implemented")
    }

    override fun aii(dst: Address, first: Imm32, second: Imm32) = when (dst) {
        is Address2 -> asm.mov(size, first, dst.withOffset(second.value().toInt() * size))
        else -> throw RuntimeException("Unknown type=$type, dst=$dst, first=$first, second=$second")
    }

    override fun air(dst: Address, first: Imm32, second: GPRegister) {
        TODO("Not yet implemented")
    }

    override fun aia(dst: Address, first: Imm32, second: Address) = when (dst) {
        is Address2 -> {
            asm.mov(size, second, temp1)
            asm.mov(size, first, Address.from(dst.base, dst.offset, temp1, ScaleFactor.from(size)))
        }
        else -> throw RuntimeException("Unknown type=$type, dst=$dst, first=$first, second=$second")
    }

    override fun ari(dst: Address, first: GPRegister, second: Imm32) = when (dst) {
        is Address2 -> asm.mov(size, first, Address.from(dst.base, dst.offset + second.value().toInt() * size))
        else -> throw RuntimeException("Unknown type=$type, dst=$dst, first=$first, second=$second")
    }

    override fun aai(dst: Address, first: Address, second: Imm32) {
        TODO("untested")
        if (dst !is Address2) {
            throw RuntimeException("Unknown type=$type, dst=$dst, first=$first, second=$second")
        }
       // asm.mov(size, first, temp1)
       // asm.mov(size, temp1, Address.from(dst.base, dst.offset + second.value().toInt() * size))
    }

    override fun aar(dst: Address, first: Address, second: GPRegister) {
        TODO("Not yet implemented")
    }

    override fun aaa(dst: Address, first: Address, second: Address) {
        TODO("Not yet implemented")
    }

    override fun default(dst: Operand, first: Operand, second: Operand) {
        throw RuntimeException("Internal error: '${StoreOnStack.NAME}' dst=$dst, first=$first, second=$second")
    }
}