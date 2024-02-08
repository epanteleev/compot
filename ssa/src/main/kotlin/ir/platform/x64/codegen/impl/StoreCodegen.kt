package ir.platform.x64.codegen.impl

import asm.x64.*
import ir.types.*
import ir.instruction.Store
import ir.platform.x64.CallConvention.POINTER_SIZE
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.codegen.utils.*


data class StoreCodegen(val type: PrimitiveType, val asm: Assembler): GPOperandVisitorUnaryOp,
    XmmOperandVisitorUnaryOp {
    private val size = type.size()

    operator fun invoke(value: Operand, pointer: Operand) {
        when (type) {
            is FloatingPointType           -> ApplyClosure(pointer, value, this as XmmOperandVisitorUnaryOp)
            is IntegerType, is PointerType -> ApplyClosure(pointer, value, this as GPOperandVisitorUnaryOp)
            else -> throw RuntimeException("Unknown type=$type, value=$value, pointer=$pointer")
        }
    }

    override fun rr(dst: GPRegister, src: GPRegister) {
        asm.mov(size, src, Address.from(dst, 0))
    }

    override fun ra(dst: GPRegister, src: Address) {
        TODO("Not yet implemented")
    }

    override fun ar(dst: Address, src: GPRegister) {
        asm.mov(POINTER_SIZE, dst, temp1)
        asm.mov(size, src, Address.from(temp1, 0))
    }

    override fun aa(dst: Address, src: Address) {
        TODO("Not yet implemented")
    }

    override fun ri(dst: GPRegister, src: Imm32) {
        asm.mov(size, src, Address.from(dst, 0))
    }

    override fun ai(dst: Address, src: Imm32) {
        when (dst) {
            is AddressLiteral -> asm.mov(size, src, dst)
            else -> TODO("Not yet implemented")
        }
    }

    override fun rrF(dst: XmmRegister, src: XmmRegister) {
        TODO("Not yet implemented")
    }

    override fun raF(dst: XmmRegister, src: Address) {
        TODO("Not yet implemented")
    }

    override fun arF(dst: Address, src: XmmRegister) {
        TODO("Not yet implemented")
    }

    override fun aaF(dst: Address, src: Address) {
        TODO("Not yet implemented")
    }

    override fun default(dst: Operand, src: Operand) {
        if (dst is GPRegister && src is XmmRegister) {
            asm.movf(size, src, Address.from(dst, 0))
        } else {
            throw RuntimeException("Internal error: '${Store.NAME}' dst=$dst, pointer=$src")
        }
    }
}