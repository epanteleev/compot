package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.Definitions.QWORD_SIZE
import ir.types.*
import ir.instruction.Int2Float
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.CallConvention.temp2
import ir.platform.x64.CallConvention.xmmTemp1
import ir.platform.x64.codegen.MacroAssembler
import ir.platform.x64.codegen.visitors.GPOperandToXmmVisitor


class Uint2FloatCodegen(toType: FloatingPointType, val fromType: UnsignedIntType, val asm: MacroAssembler) : GPOperandToXmmVisitor {
    private val toSize = toType.sizeOf()
    private val fromSize = fromType.sizeOf()

    operator fun invoke(dst: Operand, src: Operand) {
        GPOperandToXmmVisitor.apply(dst, src, this)
    }

    override fun rx(dst: XmmRegister, src: GPRegister) {
        if (fromType == Type.U64 || fromType == Type.U32) {
            cvtU64(dst, src)
            return
        }
        val temp = convertOnDemand(src)
        if (temp != null) {
            asm.cvtint2fp(TEMP_SIZE, toSize, temp, dst)
        } else {
            asm.cvtint2fp(fromSize, toSize, src, dst)
        }
    }

    override fun ax(dst: XmmRegister, src: Address) {
        if (fromType == Type.U64 || fromType == Type.U32) {
            cvtU64(dst, src)
            return
        }
        val temp = convertOnDemand(src)
        if (temp != null) {
            asm.cvtint2fp(TEMP_SIZE, toSize, temp, dst)
        } else {
            asm.cvtint2fp(fromSize, toSize, src, dst)
        }
    }

    override fun ar(dst: Address, src: GPRegister) {
        if (fromType == Type.U64 || fromType == Type.U32) {
            cvtU64(dst, src)
            return
        }
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
        if (fromType == Type.U64 || fromType == Type.U32) {
            cvtU64(dst, src)
            return
        }
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
        if (fromSize == Type.U32.sizeOf() || fromSize == Type.U64.sizeOf()) {
            return null
        }

        asm.movzext(fromSize, TEMP_SIZE, src, temp1)
        return temp1
    }

    private fun convertOnDemand(src: Address): GPRegister? { //todo code duplication
        if (fromSize == Type.U32.sizeOf() || fromSize == Type.U64.sizeOf()) {
            return null
        }

        asm.movzext(fromSize, TEMP_SIZE, src, temp1)
        return temp1
    }

    // MCVS 19.40
    private fun cvtU64(dst: XmmRegister, src: GPRegister) {
        val currentLabel = asm.currentLabel()
        val slowPath     = asm.anonLabel()
        val cont         = asm.anonLabel()
        asm.switchTo(currentLabel).let {
            if (fromType == Type.U32) { //TODO simplify for U32 ???????????????
                asm.mov(fromSize, src, src)
            }
            asm.test(QWORD_SIZE, src, src)
            asm.jcc(CondType.JS, slowPath)
        }
        currentLabel.let {
            asm.cvtint2fp(QWORD_SIZE, toSize, src, dst)
            asm.jump(cont)
        }
        asm.switchTo(slowPath).let {
            asm.copy(fromSize, src, temp1)
            asm.copy(fromSize, src, temp2)
            asm.shr(QWORD_SIZE, Imm32.of(1), temp1)
            asm.and(fromSize, Imm32.of(1), temp2)
            asm.or(QWORD_SIZE, temp1, temp2)
            asm.pxor(dst, dst)
            asm.cvtint2fp(QWORD_SIZE, toSize, temp2, dst)
            asm.addf(QWORD_SIZE, dst, dst)
        }
        asm.switchTo(cont)
    }

    private fun cvtU64(dst: XmmRegister, src: Address) {
        val currentLabel = asm.currentLabel()
        val slowPath     = asm.anonLabel()
        val cont         = asm.anonLabel()
        asm.switchTo(currentLabel).let {
            asm.mov(fromSize, src, temp1)
            if (fromType == Type.U32) { //TODO simplify for U32 ???????????????
                asm.mov(fromSize, temp1, temp1)
            }
            asm.test(QWORD_SIZE, temp1, temp1)
            asm.jcc(CondType.JS, slowPath)
        }
        currentLabel.let {
            asm.cvtint2fp(QWORD_SIZE, toSize, src, dst)
            asm.jump(cont)
        }
        asm.switchTo(slowPath).let {
            asm.mov(fromSize, src, temp2)
            asm.shr(QWORD_SIZE, Imm32.of(1), temp1)
            asm.and(fromSize, Imm32.of(1), temp2)
            asm.or(QWORD_SIZE, temp1, temp2)
            asm.pxor(dst, dst)
            asm.cvtint2fp(QWORD_SIZE, toSize, temp2, dst)
            asm.addf(QWORD_SIZE, dst, dst)
        }
        asm.switchTo(cont)
    }

    private fun cvtU64(dst: Address, src: Address) {
        cvtU64(xmmTemp1, src)
        asm.movf(toSize, xmmTemp1, dst)
    }

    private fun cvtU64(dst: Address, src: GPRegister) {
        cvtU64(xmmTemp1, src)
        asm.movf(toSize, xmmTemp1, dst)
    }

    companion object {
        private val TEMP_SIZE = Type.I32.sizeOf()
    }
}