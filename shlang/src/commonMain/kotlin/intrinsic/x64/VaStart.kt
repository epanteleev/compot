package intrinsic.x64

import types.*
import asm.Operand
import asm.x64.Address
import asm.x64.Address2
import asm.x64.GPRegister.*
import asm.x64.Imm32
import common.assertion
import ir.Definitions.POINTER_SIZE
import ir.Definitions.WORD_SIZE

import ir.intrinsic.IntrinsicImplementor
import ir.platform.MacroAssembler
import ir.platform.x64.codegen.X64MacroAssembler
import typedesc.TypeDesc


class VaStart() : IntrinsicImplementor("va_start", listOf()) {
    override fun <Masm : MacroAssembler> implement(masm: Masm, inputs: List<Operand>) {
        assertion(masm is X64MacroAssembler) { "masm must be X64MacroAssembler" }
        masm as X64MacroAssembler
        assertion(inputs.size == 2) { "va_start must have 2 arguments" }

        val vaStart = inputs[0]
        assertion(vaStart is Address2) { "va_start must be Reg64" }
        vaStart as Address2

        val vaInit = inputs[1]
        assertion(vaInit is Address2) { "va_start must be Reg64" }
        vaInit as Address2

        // vaStart.gp_offset = 8
        masm.mov(WORD_SIZE, Imm32.of(8), vaStart)

        // vaStart.fp_offset = 48
        masm.mov(WORD_SIZE, Imm32.of(48), Address.from(vaStart.base, vaStart.offset + 4))

        // vaStart.overflow_arg_area = lea [rbp + 16]
        // TODO correct only for -fno-omit-frame-pointer mode
        masm.lea(POINTER_SIZE, Address.from(rbp, 16), rax)
        masm.mov(POINTER_SIZE, rax, Address.from(vaStart.base, vaStart.offset + 8))

        // vaStart.reg_save_area = lea $vaInit
        masm.lea(POINTER_SIZE, vaInit, rax) //TODO get register from inputs
        masm.mov(POINTER_SIZE, rax, Address.from(vaStart.base, vaStart.offset + 16))
    }

    companion object {
        val vaList = CStructType("va_list", arrayListOf(
            FieldMember("gp_offset", TypeDesc.from(INT, listOf())),
            FieldMember("fp_offset", TypeDesc.from(INT, listOf())),
            FieldMember("overflow_arg_area", TypeDesc.from(CPointer(CHAR), listOf())),
            FieldMember("reg_save_area", TypeDesc.from(CPointer(CHAR), listOf())))
        )
    }
}