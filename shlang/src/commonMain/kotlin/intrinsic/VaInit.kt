package intrinsic

import asm.x64.*
import types.*
import asm.x64.GPRegister.*
import asm.x64.XmmRegister.*
import common.assertion
import ir.Definitions
import ir.Definitions.QWORD_SIZE
import ir.intrinsic.IntrinsicProvider
import ir.platform.MacroAssembler
import ir.platform.x64.codegen.X64MacroAssembler
import typedesc.TypeDesc


class VaInit(private val firstArgType: CType): IntrinsicProvider("va_init") { //TODO consume all arguments
    override fun <Masm : MacroAssembler> implement(masm: Masm, inputs: List<Operand>) {
        assertion(inputs.size == 1) { "va_init must have 1 arguments" }

        val vaInit = inputs.first()
        assertion(vaInit is Address2) { "va_start must be offset(%base)" }
        vaInit as Address2

        when (masm) {
            is X64MacroAssembler -> implementX64(masm, vaInit)
            else -> error("Unsupported assembler: ${masm.platform()}")
        }
    }

    private fun implementX64(masm: X64MacroAssembler, vaInit: Address2) {
        val currentLabel = masm.currentLabel()
        val gprBlock = masm.anonLabel()
        masm.switchTo(currentLabel)
        masm.apply {
            test(Definitions.BYTE_SIZE, rax, rax)
            jcc(CondFlagType.EQ, gprBlock)

            val isGPOperand = isGPOperand(firstArgType)
            if (isGPOperand) {
                movf(QWORD_SIZE, xmm0, Address.from(vaInit.base, vaInit.offset + 6 * QWORD_SIZE))
            }

            movf(QWORD_SIZE, xmm1, Address.from(vaInit.base, vaInit.offset + 7 * QWORD_SIZE))
            movf(QWORD_SIZE, xmm2, Address.from(vaInit.base, vaInit.offset + 8 * QWORD_SIZE))
            movf(QWORD_SIZE, xmm3, Address.from(vaInit.base, vaInit.offset + 9 * QWORD_SIZE))
            movf(QWORD_SIZE, xmm4, Address.from(vaInit.base, vaInit.offset + 10 * QWORD_SIZE))
            movf(QWORD_SIZE, xmm5, Address.from(vaInit.base, vaInit.offset + 11 * QWORD_SIZE))
            movf(QWORD_SIZE, xmm6, Address.from(vaInit.base, vaInit.offset + 12 * QWORD_SIZE))
            movf(QWORD_SIZE, xmm7, Address.from(vaInit.base, vaInit.offset + 13 * QWORD_SIZE))

            switchTo(gprBlock)
            if (!isGPOperand) {
                mov(QWORD_SIZE, rdi, Address.from(vaInit.base, vaInit.offset))
            }
            mov(QWORD_SIZE, rsi, Address.from(vaInit.base, vaInit.offset + QWORD_SIZE))
            mov(QWORD_SIZE, rdx, Address.from(vaInit.base, vaInit.offset + 2 * QWORD_SIZE))
            mov(QWORD_SIZE, rcx, Address.from(vaInit.base, vaInit.offset + 3 * QWORD_SIZE))
            mov(QWORD_SIZE, r8, Address.from(vaInit.base, vaInit.offset + 4 * QWORD_SIZE))
            mov(QWORD_SIZE, r9, Address.from(vaInit.base, vaInit.offset + 5 * QWORD_SIZE))
        }
    }

    companion object {
        fun isGPOperand(type: CType): Boolean = when (type) {
            is BOOL, is AnyCInteger, is CPointer, is CEnumType -> true
            else -> false
        }

        fun isFPOperand(type: CType): Boolean = when (type) {
            is AnyCFloat -> true
            else -> false
        }

        val vaInit = CStructType.create("va_init", arrayListOf(
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
            )
        )
    }
}