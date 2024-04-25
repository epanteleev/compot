package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.types.*
import ir.instruction.Bitcast
import ir.platform.x64.codegen.utils.ApplyClosure
import ir.platform.x64.codegen.utils.GPOperandsVisitorUnaryOp


data class BitcastCodegen (val type: PrimitiveType, val asm: Assembler): GPOperandsVisitorUnaryOp {
    private val size = type.size() // toSize

    operator fun invoke(dst: Operand, src: Operand) {
        when (type) {
            is IntegerType, is PointerType -> ApplyClosure(dst, src, this as GPOperandsVisitorUnaryOp)
            else -> throw RuntimeException("Unknown '${Bitcast.NAME}' type=$type, dst=$dst, src=$src")
        }
    }

    override fun rr(dst: GPRegister, src: GPRegister) {
        if (dst == src) {
            return
        }

        asm.mov(size, src, dst)
    }

    override fun ra(dst: GPRegister, src: Address) {
        asm.mov(size, src, dst)
    }

    override fun ar(dst: Address, src: GPRegister) {
        TODO("Not yet implemented")
    }

    override fun aa(dst: Address, src: Address) {
        TODO("Not yet implemented")
    }

    override fun ri(dst: GPRegister, src: Imm32) {
        TODO("Not yet implemented")
    }

    override fun ai(dst: Address, src: Imm32) {
        TODO("Not yet implemented")
    }

    override fun default(dst: Operand, src: Operand) {
        throw RuntimeException("Internal error: '${Bitcast.NAME}' dst=$dst, src=$$src")
    }
}