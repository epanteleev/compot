package ir.platform.x64.codegen.impl

import asm.x64.Operand
import asm.x64.*
import ir.Definitions.POINTER_SIZE
import ir.types.*
import ir.instruction.Load
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.codegen.visitors.*


internal class LoadIntCodegen(val type: PrimitiveType, val asm: Assembler): GPOperandsVisitorUnaryOp {
    private val size = type.sizeOf()

    operator fun invoke(value: Operand, pointer: Operand) = when (type) {
        is IntegerType, is PtrType -> GPOperandsVisitorUnaryOp.apply(value, pointer, this)
        else -> throw RuntimeException("Unknown type=$type, value=$value, pointer=$pointer")
    }

    override fun rr(dst: GPRegister, src: GPRegister) {
        asm.mov(size, Address.from(src, 0), dst)
    }

    override fun ra(dst: GPRegister, src: Address) {
        asm.mov(POINTER_SIZE, src, temp1)
        asm.mov(size, Address.from(temp1, 0), dst)
    }

    override fun ar(dst: Address, src: GPRegister) {
        asm.mov(size, Address.from(src, 0), temp1)
        asm.mov(size, temp1, dst)
    }

    override fun aa(dst: Address, src: Address) {
        asm.mov(POINTER_SIZE, src, temp1)
        asm.mov(size, Address.from(temp1, 0), temp1)
        asm.mov(size, temp1, dst)
    }

    override fun ri(dst: GPRegister, src: Imm) {
        TODO("Not yet implemented")
    }

    override fun ai(dst: Address, src: Imm) {
        TODO("Not yet implemented")
    }

    override fun default(dst: Operand, src: Operand) {
        throw RuntimeException("Internal error: '${Load.NAME}' dst=$dst, pointer=$src")
    }
}