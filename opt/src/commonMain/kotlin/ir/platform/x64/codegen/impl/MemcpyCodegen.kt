package ir.platform.x64.codegen.impl

import asm.x64.Operand
import asm.x64.*
import ir.Definitions.POINTER_SIZE
import ir.Definitions.WORD_SIZE
import ir.instruction.Memcpy
import ir.value.constant.UnsignedIntegerConstant
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.codegen.visitors.GPOperandsVisitorUnaryOp


internal class MemcpyCodegen(val length: UnsignedIntegerConstant, val asm: Assembler): GPOperandsVisitorUnaryOp {
    operator fun invoke(dst: Operand, src: Operand) {
        GPOperandsVisitorUnaryOp.apply(dst, src, this)
    }

    override fun rr(dst: GPRegister, src: GPRegister) {
        // Not so efficient, but simple
        val iterations8 = length.value() / POINTER_SIZE.toUInt()
        val remains8 = length.value() % POINTER_SIZE.toUInt()
        for (i in 0 until iterations8.toInt()) {
            asm.mov(POINTER_SIZE, Address.from(src, i * POINTER_SIZE), temp1)
            asm.mov(POINTER_SIZE, temp1, Address.from(dst, i * POINTER_SIZE))
        }
        val base = iterations8.toInt() * POINTER_SIZE
        val iterations4 = remains8 / WORD_SIZE.toUInt()
        val remains4 = remains8 % WORD_SIZE.toUInt()
        for (i in 0 until iterations4.toInt()) {
            asm.mov(WORD_SIZE, Address.from(src, base + i * WORD_SIZE), temp1)
            asm.mov(WORD_SIZE, temp1, Address.from(dst, base + i * WORD_SIZE))
        }
        val base4 = base + iterations4.toInt() * WORD_SIZE
        for (i in 0 until remains4.toInt()) {
            asm.mov(1, Address.from(src, base4 + i), temp1)
            asm.mov(1, temp1, Address.from(dst, base4 + i))
        }
    }

    override fun ra(dst: GPRegister, src: Address) = default(dst, src)

    override fun ar(dst: Address, src: GPRegister) = default(dst, src)

    override fun aa(dst: Address, src: Address) = default(dst, src)

    override fun ri(dst: GPRegister, src: Imm) = default(dst, src)

    override fun ai(dst: Address, src: Imm) = default(dst, src)

    override fun default(dst: Operand, src: Operand) {
        throw RuntimeException("Internal error: '${Memcpy.NAME}' dst=$dst, src=$src")
    }
}