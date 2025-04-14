package ir.platform.x64.codegen.impl

import asm.x64.Operand
import asm.x64.*
import ir.types.*
import ir.instruction.Store
import ir.platform.x64.codegen.visitors.*
import ir.platform.x64.CallConvention.temp1


internal class MoveIntCodegen(val type: PrimitiveType, val asm: Assembler): GPOperandsVisitorUnaryOp {
    private val size = type.sizeOf()

    operator fun invoke(dst: Operand, value: Operand) = when (type) {
        is IntegerType, is PtrType -> GPOperandsVisitorUnaryOp.apply(dst, value, this)
        else -> throw RuntimeException("Unknown type=$type, value=$value, pointer=$dst")
    }

    override fun ar(dst: Address, src: GPRegister) {
        asm.mov(size, src, dst)
    }

    override fun aa(dst: Address, src: Address) {
        if (dst == src) {
            return
        }

        asm.mov(size, src, temp1)
        asm.mov(size, temp1, dst)
    }

    override fun ai(dst: Address, src: Imm) {
        if (Imm.canBeImm32(src.value())) {
            asm.mov(size, src.asImm32(), dst)
        } else {
            asm.mov(size, src, temp1)
            asm.mov(size, temp1, dst)
        }
    }

    override fun ri(dst: GPRegister, src: Imm) = default(dst, src)
    override fun rr(dst: GPRegister, src: GPRegister) = default(dst, src)
    override fun ra(dst: GPRegister, src: Address) = default(dst, src)
    override fun default(dst: Operand, src: Operand) {
        throw RuntimeException("Internal error: '${Store.NAME}' dst=$dst, src=$src")
    }
}