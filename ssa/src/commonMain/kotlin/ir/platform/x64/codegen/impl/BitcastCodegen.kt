package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.types.*
import ir.instruction.Bitcast
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.codegen.visitors.GPOperandsVisitorUnaryOp


data class BitcastCodegen (val type: PrimitiveType, val asm: Assembler): GPOperandsVisitorUnaryOp {
    private val size = type.sizeOf() // toSize

    operator fun invoke(dst: Operand, src: Operand) {
        when (type) {
            is IntegerType, is PointerType -> GPOperandsVisitorUnaryOp.apply(dst, src, this)
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
        asm.mov(size, src, dst)
    }

    override fun aa(dst: Address, src: Address) {
        asm.mov(size, src, temp1)
        asm.mov(size, temp1, dst)
    }

    override fun ri(dst: GPRegister, src: Imm32) {
        asm.mov(size, src, dst)
    }

    override fun ai(dst: Address, src: Imm32) {
        asm.mov(size, src, dst)
    }

    override fun default(dst: Operand, src: Operand) {
        throw RuntimeException("Internal error: '${Bitcast.NAME}' dst=$dst, src=$$src")
    }
}