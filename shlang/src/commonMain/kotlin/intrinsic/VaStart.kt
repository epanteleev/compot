package intrinsic

import types.*
import asm.Operand
import asm.x64.Address
import asm.x64.Address2
import asm.x64.GPRegister.*
import asm.x64.Imm32
import common.assertion
import intrinsic.VaInit.Companion.isFPOperand
import intrinsic.VaInit.Companion.isGPOperand
import ir.Definitions.POINTER_SIZE
import ir.Definitions.QWORD_SIZE
import ir.Definitions.WORD_SIZE
import ir.intrinsic.IntrinsicProvider
import ir.platform.MacroAssembler
import ir.platform.x64.codegen.X64MacroAssembler
import typedesc.TypeDesc


class VaStart(private val arguments: List<TypeDesc>) : IntrinsicProvider("va_start") {
    init {
        require(arguments.isNotEmpty()) { "va_start must have 2 arguments" }
    }

    override fun <Masm : MacroAssembler> implement(masm: Masm, inputs: List<Operand>) {
        assertion(inputs.size == 2) { "va_start must have 2 arguments" }

        val vaStart = inputs[0]
        assertion(vaStart is Address2) { "va_start must be offset(%base)" }
        vaStart as Address2

        val vaInit = inputs[1]
        assertion(vaInit is Address2) { "va_start must be offset(%base)" }
        vaInit as Address2

        when (masm) {
            is X64MacroAssembler -> implementX64(masm, vaStart, vaInit)
            else -> error("Unsupported assembler: ${masm.platform()}")
        }
    }

    private fun implementX64(masm: X64MacroAssembler, vaStart: Address2, vaInit: Address2) {
        val numberOfGPArgs = arguments.count { isGPOperand(it.cType()) }
        val numberOfFPArgs = arguments.count { isFPOperand(it.cType()) }
        masm.apply {
            // vaStart.gp_offset = 8
            mov(WORD_SIZE, Imm32.of(numberOfGPArgs * QWORD_SIZE), vaStart)

            // vaStart.fp_offset = 48
            mov(WORD_SIZE, Imm32.of(48 + numberOfFPArgs * QWORD_SIZE), Address.from(vaStart.base, vaStart.offset + FP_OFFSET_OFFSET))

            // vaStart.overflow_arg_area = lea [rbp + 16]
            // TODO correct only for -fno-omit-frame-pointer mode
            lea(POINTER_SIZE, Address.from(rbp, 16), rax)
            mov(POINTER_SIZE, rax, Address.from(vaStart.base, vaStart.offset + OVERFLOW_ARG_AREA_OFFSET))

            // vaStart.reg_save_area = lea $vaInit
            lea(POINTER_SIZE, vaInit, rax) //TODO get register from inputs
            mov(POINTER_SIZE, rax, Address.from(vaStart.base, vaStart.offset + REG_SAVE_AREA_OFFSET))
        }
    }

    companion object {
        const val REG_SAVE_AREA_SIZE    = 40
        const val FP_REG_SAVE_AREA_SIZE = 56

        const val GP_OFFSET_IDX         = 0
        const val FP_OFFSET_IDX         = 1
        const val OVERFLOW_ARG_AREA_IDX = 2
        const val REG_SAVE_AREA_IDX     = 3

        val vaList = CStructType.create("va_list", arrayListOf(
            FieldMember("gp_offset",         TypeDesc.from(INT, listOf())),
            FieldMember("fp_offset",         TypeDesc.from(INT, listOf())),
            FieldMember("overflow_arg_area", TypeDesc.from(CPointer(CHAR), listOf())),
            FieldMember("reg_save_area",     TypeDesc.from(CPointer(CHAR), listOf())))
        )

        val FP_OFFSET_OFFSET         = vaList.offset(FP_OFFSET_IDX)
        val OVERFLOW_ARG_AREA_OFFSET = vaList.offset(OVERFLOW_ARG_AREA_IDX)
        val REG_SAVE_AREA_OFFSET     = vaList.offset(REG_SAVE_AREA_IDX)
    }
}