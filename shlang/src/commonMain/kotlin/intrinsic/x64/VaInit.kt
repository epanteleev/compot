package intrinsic.x64

import asm.Operand
import asm.x64.Address
import asm.x64.Address2
import asm.x64.CondType
import asm.x64.GPRegister.*
import asm.x64.XmmRegister.*
import common.assertion
import ir.Definitions.BYTE_SIZE
import ir.Definitions.QWORD_SIZE
import ir.Definitions.QWORD_SIZE
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


        masm.test(BYTE_SIZE, rax, rax)
        val currentLabel = masm.currentLabel()
        val gprBlock = masm.anonLabel()
        masm.switchTo(currentLabel)

        masm.jcc(CondType.JE, gprBlock)

        masm.movf(QWORD_SIZE, xmm0, Address.from(vaInit.base, vaInit.offset + 6 * QWORD_SIZE))
        masm.movf(QWORD_SIZE, xmm1, Address.from(vaInit.base, vaInit.offset + 7 * QWORD_SIZE))
        masm.movf(QWORD_SIZE, xmm2, Address.from(vaInit.base, vaInit.offset + 8 * QWORD_SIZE))
        masm.movf(QWORD_SIZE, xmm3, Address.from(vaInit.base, vaInit.offset + 9 * QWORD_SIZE))
        masm.movf(QWORD_SIZE, xmm4, Address.from(vaInit.base, vaInit.offset + 10 * QWORD_SIZE))
        masm.movf(QWORD_SIZE, xmm5, Address.from(vaInit.base, vaInit.offset + 11 * QWORD_SIZE))
        masm.movf(QWORD_SIZE, xmm6, Address.from(vaInit.base, vaInit.offset + 12 * QWORD_SIZE))
        masm.movf(QWORD_SIZE, xmm7, Address.from(vaInit.base, vaInit.offset + 13 * QWORD_SIZE))

        masm.switchTo(gprBlock)
        masm.mov(QWORD_SIZE, rsi, Address.from(vaInit.base, vaInit.offset + QWORD_SIZE))
        masm.mov(QWORD_SIZE, rdx, Address.from(vaInit.base, vaInit.offset + 2 * QWORD_SIZE))
        masm.mov(QWORD_SIZE, rcx, Address.from(vaInit.base, vaInit.offset + 3 * QWORD_SIZE))
        masm.mov(QWORD_SIZE, r8, Address.from(vaInit.base, vaInit.offset + 4 * QWORD_SIZE))
        masm.mov(QWORD_SIZE, r9, Address.from(vaInit.base, vaInit.offset + 5 * QWORD_SIZE))
    }

    companion object {
        val vaInit = CStructType("va_init", arrayListOf(
            FieldMember("xmm0", TypeDesc.from(DOUBLE, listOf())),
            FieldMember("xmm1", TypeDesc.from(DOUBLE, listOf())),
            FieldMember("xmm2", TypeDesc.from(DOUBLE, listOf())),
            FieldMember("xmm3", TypeDesc.from(DOUBLE, listOf())),
            FieldMember("xmm4", TypeDesc.from(DOUBLE, listOf())),
            FieldMember("xmm5", TypeDesc.from(DOUBLE, listOf())),
            FieldMember("xmm6", TypeDesc.from(DOUBLE, listOf())),
            FieldMember("xmm7", TypeDesc.from(DOUBLE, listOf())),
            FieldMember("rdi", TypeDesc.from(LONG, listOf())),
            FieldMember("rsi", TypeDesc.from(LONG, listOf())),
            FieldMember("rdx", TypeDesc.from(LONG, listOf())),
            FieldMember("rcx", TypeDesc.from(LONG, listOf())),
            FieldMember("r8", TypeDesc.from(LONG, listOf())),
            FieldMember("r9", TypeDesc.from(LONG, listOf())),
        ))
    }
}