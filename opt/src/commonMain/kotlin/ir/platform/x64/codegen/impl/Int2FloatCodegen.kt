package ir.platform.x64.codegen.impl

import asm.Operand
import asm.x64.*
import ir.types.*
import ir.instruction.Int2Float
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.CallConvention.xmmTemp1
import ir.platform.x64.codegen.visitors.GPOperandToXmmVisitor


internal class Int2FloatCodegen(toType: FloatingPointType, fromType: SignedIntType, val asm: Assembler) : GPOperandToXmmVisitor {
    private val toSize = toType.sizeOf()
    private val fromSize = fromType.sizeOf()

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
        if (fromSize == I32Type.sizeOf() || fromSize == I64Type.sizeOf()) {
            return null
        }

        asm.movsext(fromSize, TEMP_SIZE, src, temp1)
        return temp1
    }

    private fun convertOnDemand(src: Address): GPRegister? { //todo code duplication
        if (fromSize == I32Type.sizeOf() || fromSize == I64Type.sizeOf()) {
            return null
        }

        asm.movsext(fromSize, TEMP_SIZE, src, temp1)
        return temp1
    }

    companion object {
        private val TEMP_SIZE = I32Type.sizeOf()
    }
}