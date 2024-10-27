package intrinsic.x64

import asm.Operand
import ir.intrinsic.IntrinsicImplementor
import ir.platform.MacroAssembler
import typedesc.TypeDesc
import types.*


class VaInit(): IntrinsicImplementor("va_init", listOf()) {
    override fun <Masm : MacroAssembler> implement(masm: Masm, inputs: List<Operand>) {
        TODO("Not yet implemented")
    }

    companion object {
        val vaList = CStructType("va_list", arrayListOf(
            FieldMember("gp_offset", TypeDesc.from(INT, listOf())),
            FieldMember("fp_offset", TypeDesc.from(INT, listOf())),
            FieldMember("overflow_arg_area", TypeDesc.from(CPointer(CHAR), listOf())),
            FieldMember("reg_save_area", TypeDesc.from(CPointer(CHAR), listOf())))
        )

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