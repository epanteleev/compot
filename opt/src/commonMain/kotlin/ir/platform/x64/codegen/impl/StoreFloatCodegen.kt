package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.types.*
import ir.Definitions.POINTER_SIZE
import ir.instruction.Store
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.CallConvention.temp2
import ir.platform.x64.codegen.visitors.XmmOperandsVisitorUnaryOp


class StoreFloatCodegen(val type: FloatingPointType, val asm: Assembler): XmmOperandsVisitorUnaryOp {
    private val size = type.sizeOf()

    operator fun invoke(value: Operand, pointer: Operand) {
        XmmOperandsVisitorUnaryOp.apply(pointer, value, this)
    }

    override fun rr(dst: XmmRegister, src: XmmRegister) {
        TODO("Not yet implemented")
    }

    override fun ra(dst: XmmRegister, src: Address) {
        TODO("Not yet implemented")
    }

    override fun ar(dst: Address, src: XmmRegister) {
        asm.mov(POINTER_SIZE, dst, temp1)
        asm.movf(size, src, Address.from(temp1, 0))
    }

    override fun aa(dst: Address, src: Address) {
        asm.mov(POINTER_SIZE, dst, temp1)
        asm.mov(size, src, temp2)
        asm.mov(size, temp2, Address.from(temp1, 0))
    }

    override fun default(dst: Operand, src: Operand) {
        when (dst) {
            is GPRegister if src is XmmRegister -> {
                asm.movf(size, src, Address.from(dst, 0))
            }
            is GPRegister if src is Address -> {
                asm.mov(size, src, temp1)
                asm.mov(size, temp1, Address.from(dst, 0))
            }
            else -> throw RuntimeException("Internal error: '${Store.NAME}' dst=$dst, src=$src")
        }
    }
}