package ir.platform.x64.codegen.impl

import asm.x64.Operand
import asm.x64.*
import ir.types.*
import ir.instruction.Store
import ir.Definitions.POINTER_SIZE
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.CallConvention.temp2
import ir.platform.x64.codegen.visitors.*


internal class StoreIntCodegen(val type: PrimitiveType, val asm: Assembler): GPOperandsVisitorUnaryOp {
    private val size = type.sizeOf()

    operator fun invoke(value: Operand, pointer: Operand) = when (type) {
        is IntegerType, is PtrType -> GPOperandsVisitorUnaryOp.apply(pointer, value, this)
        else -> throw RuntimeException("Unknown type=$type, value=$value, pointer=$pointer")
    }

    override fun rr(dst: GPRegister, src: GPRegister) {
        asm.mov(size, src, Address.from(dst, 0))
    }

    override fun ra(dst: GPRegister, src: Address) {
        asm.mov(size, src, temp1)
        asm.mov(size, temp1, Address.from(dst, 0))
    }

    override fun ar(dst: Address, src: GPRegister) {
        asm.mov(POINTER_SIZE, dst, temp1)
        asm.mov(size, src, Address.from(temp1, 0))
    }

    override fun aa(dst: Address, src: Address) {
        asm.mov(POINTER_SIZE, dst, temp1)
        asm.mov(size, src, temp2)
        asm.mov(size, temp2, Address.from(temp1, 0))
    }

    override fun ri(dst: GPRegister, src: Imm32) {
        asm.mov(size, src, Address.from(dst, 0))
    }

    override fun ai(dst: Address, src: Imm32) {
        asm.mov(POINTER_SIZE, dst, temp1)
        asm.mov(size, src, Address.from(temp1, 0))
    }

    override fun default(dst: Operand, src: Operand) {
        if (dst is GPRegister && src is XmmRegister) {
            asm.movf(size, src, Address.from(dst, 0))
        } else if (dst is GPRegister && src is Address) {
            asm.mov(size, src, temp1)
            asm.mov(size, temp1, Address.from(dst, 0))
        } else {
            throw RuntimeException("Internal error: '${Store.NAME}' dst=$dst, src=$src")
        }
    }
}