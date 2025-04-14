package ir.platform.x64.codegen.impl

import asm.x64.Operand
import asm.x64.*
import ir.types.PrimitiveType
import ir.instruction.IntCompare
import ir.platform.x64.CallConvention.temp1
import ir.platform.x64.CallConvention.temp2
import ir.platform.x64.codegen.X64MacroAssembler
import ir.platform.x64.codegen.visitors.CmpGPOperandVisitor


internal class IntCmpCodegen(val type: PrimitiveType, val asm: X64MacroAssembler): CmpGPOperandVisitor {
    private val size: Int = type.sizeOf()

    operator fun invoke(first: Operand, second: Operand) {
        CmpGPOperandVisitor.apply(first, second, this)
    }

    override fun rr(first: GPRegister, second: GPRegister) {
        asm.cmp(size, second, first)
    }

    override fun ra(first: GPRegister, second: Address) {
        asm.cmp(size, second, first)
    }

    override fun ar(first: Address, second: GPRegister) {
        asm.cmp(size, second, first)
    }

    override fun aa(first: Address, second: Address) {
        asm.mov(size, first, temp1)
        asm.cmp(size, second, temp1)
    }

    override fun ai(first: Address, second: Imm) {
        if (Imm.canBeImm32(second.value())) {
            asm.cmp(size, second.asImm32(), first)
        } else {
            asm.copy(size, second, temp1)
            asm.cmp(size, temp1, first)
        }
    }

    override fun ia(first: Imm, second: Address) {
        asm.copy(size, first, temp1)
        asm.cmp(size, second, temp1)
    }

    override fun ir(first: Imm, second: GPRegister) {
        asm.copy(size, first, temp1)
        asm.cmp(size, second, temp1)
    }

    override fun ri(first: GPRegister, second: Imm) {
        if (Imm.canBeImm32(second.value())) {
            asm.cmp(size, second.asImm32(), first)
        } else {
            asm.copy(size, second, temp1)
            asm.cmp(size, temp1, first)
        }
    }

    override fun ii(first: Imm, second: Imm) {
        asm.copy(size, first, temp1)
        asm.copy(size, second, temp2)
        asm.cmp(size, temp2, temp1)
    }

    override fun default(first: Operand, second: Operand) {
        throw RuntimeException("Internal error: '${IntCompare.NAME}' first=$first, second=$second")
    }
}