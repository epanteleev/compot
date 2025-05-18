package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.Definitions.POINTER_SIZE
import ir.instruction.Load
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.codegen.visitors.XmmOperandsVisitorUnaryOp
import ir.types.FloatingPointType


internal class LoadFloatCodegen(type: FloatingPointType, val asm: Assembler): XmmOperandsVisitorUnaryOp {
    private val size = type.sizeOf()

    operator fun invoke(value: Operand, pointer: Operand) {
        XmmOperandsVisitorUnaryOp.apply(value, pointer, this)
    }

    override fun rr(dst: XmmRegister, src: XmmRegister) {
        TODO("Not yet implemented")
    }

    override fun ra(dst: XmmRegister, src: Address) {
        asm.mov(POINTER_SIZE, src, temp1)
        asm.movf(size, Address.from(temp1, 0), dst)
    }

    override fun ar(dst: Address, src: XmmRegister) {
        TODO("Not yet implemented")
    }

    override fun aa(dst: Address, src: Address) {
        TODO("Not yet implemented")
    }

    override fun default(dst: Operand, src: Operand) {
        if (dst is XmmRegister && src is GPRegister) { //TODO: add support for other cases
            asm.movf(size, Address.from(src, 0), dst)
            return
        }
        throw RuntimeException("Internal error: '${Load.NAME}' dst=$dst, pointer=$src")
    }
}