package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.types.*
import ir.instruction.Int2Float
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.CallConvention.xmmTemp1
import ir.platform.x64.codegen.visitors.GPOperandToXmmVisitor


class Int2FloatCodegen(toType: FloatingPointType, private val fromType: IntegerType, val asm: Assembler) : GPOperandToXmmVisitor {
    private val toSize = toType.size()
    private val fromSize = fromType.size()

    operator fun invoke(dst: Operand, src: Operand) {
        GPOperandToXmmVisitor.apply(dst, src, this)
    }

    override fun rx(dst: XmmRegister, src: GPRegister) {
        val temp = convertOnDemand(src)
        if (temp != null) {
            asm.cvtint2fp(TEMP_SIZE, toSize, temp, dst)
        } else {
            asm.cvtint2fp(fromSize, toSize, src, dst)
        }
    }

    override fun ax(dst: XmmRegister, src: Address) {
        val temp = convertOnDemand(src)
        if (temp != null) {
            asm.cvtint2fp(TEMP_SIZE, toSize, temp, dst)
        } else {
            asm.cvtint2fp(fromSize, toSize, src, dst)
        }
    }

    override fun ar(dst: Address, src: GPRegister) {
        val temp = convertOnDemand(src)
        if (temp != null) {
            asm.cvtint2fp(TEMP_SIZE, toSize, temp, xmmTemp1)
            asm.movf(toSize, xmmTemp1, dst)
        } else {
            asm.cvtint2fp(fromSize, toSize, src, xmmTemp1)
            asm.movf(toSize, xmmTemp1, dst)
        }
    }

    override fun aa(dst: Address, src: Address) {
        val temp = convertOnDemand(src)
        if (temp != null) {
            asm.cvtint2fp(TEMP_SIZE, toSize, temp, xmmTemp1)
            asm.movf(toSize, xmmTemp1, dst)
        } else {
            asm.cvtint2fp(fromSize, toSize, src, xmmTemp1)
            asm.movf(toSize, xmmTemp1, dst)
        }
    }

    override fun default(dst: Operand, src: Operand) {
        throw RuntimeException("Internal error: '${Int2Float.NAME}' dst=$dst, src=$src")
    }

    private fun convertOnDemand(src: GPRegister): GPRegister? {
        when (fromType) {
            is SignedIntType -> {
                if (fromSize == Type.I32.size() || fromSize == Type.I64.size()) {
                    return null
                }

                asm.movsext(fromSize, TEMP_SIZE, src, temp1)
                return temp1
            }

            is UnsignedIntType -> {
                if (fromSize == Type.U32.size() || fromSize == Type.U64.size()) {
                    return null
                }

                asm.movzext(fromSize, TEMP_SIZE, src, temp1)
                return temp1
            }

            else -> {
                assert(false) { "always unreachable, fromType=$fromType" }
                return null
            }
        }
    }

    private fun convertOnDemand(src: Address): GPRegister? { //todo code duplication
        when (fromType) {
            is SignedIntType -> {
                if (fromSize == Type.I32.size() || fromSize == Type.I64.size()) {
                    return null
                }

                asm.movsext(fromSize, TEMP_SIZE, src, temp1)
                return temp1
            }

            is UnsignedIntType -> {
                if (fromSize == Type.U32.size() || fromSize == Type.U64.size()) {
                    return null
                }

                asm.movzext(fromSize, TEMP_SIZE, src, temp1)
                return temp1
            }

            else -> {
                assert(false) { "always unreachable, fromType=$fromType" }
                return null
            }
        }
    }

    companion object {
        private val TEMP_SIZE = Type.I32.size()
    }
}