package intrinsic.x64

import asm.Operand
import asm.x64.Address
import asm.x64.Address2
import asm.x64.GPRegister.*
import common.assertion
import ir.Definitions.POINTER_SIZE
import ir.intrinsic.IntrinsicImplementor
import ir.platform.MacroAssembler
import ir.platform.x64.codegen.X64MacroAssembler
import typedesc.TypeDesc
import types.*


class VaInit(): IntrinsicImplementor("va_init", listOf()) {
    override fun <Masm : MacroAssembler> implement(masm: Masm, inputs: List<Operand>) {
        assertion(masm is X64MacroAssembler) { "masm must be X64MacroAssembler" }
        masm as X64MacroAssembler
        assertion(inputs.size == 1) { "va_init must have 1 arguments" }

        val vaInit = inputs.first()
        assertion(vaInit is Address2) { "va_init must be Reg64" }
        vaInit as Address2

        masm.mov(POINTER_SIZE, rdi, Address.from(vaInit.base, vaInit.offset))
        masm.mov(POINTER_SIZE, rsi, Address.from(vaInit.base, vaInit.offset + POINTER_SIZE))
        masm.mov(POINTER_SIZE, rdx, Address.from(vaInit.base, vaInit.offset + 2 * POINTER_SIZE))
        masm.mov(POINTER_SIZE, rcx, Address.from(vaInit.base, vaInit.offset + 3 * POINTER_SIZE))
        masm.mov(POINTER_SIZE, r8, Address.from(vaInit.base, vaInit.offset + 4 * POINTER_SIZE))
        masm.mov(POINTER_SIZE, r9, Address.from(vaInit.base, vaInit.offset + 5 * POINTER_SIZE))
    }

    companion object {
        val vaInit = CStructType("va_init", arrayListOf(
            FieldMember("rdi", TypeDesc.from(LONG, listOf())),
            FieldMember("rsi", TypeDesc.from(LONG, listOf())),
            FieldMember("rdx", TypeDesc.from(LONG, listOf())),
            FieldMember("rcx", TypeDesc.from(LONG, listOf())),
            FieldMember("r8", TypeDesc.from(LONG, listOf())),
            FieldMember("r9", TypeDesc.from(LONG, listOf()))
        ))
    }
}