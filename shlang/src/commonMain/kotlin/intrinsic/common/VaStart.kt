package intrinsic.common

import types.*
import typedesc.TypeDesc


class VaStart {

    companion object {
        val vaList = CStructType("va_list", listOf(
            FieldMember("gp_offset", TypeDesc.from(INT, listOf())),
            FieldMember("fp_offset", TypeDesc.from(INT, listOf())),
            FieldMember("overflow_arg_area", TypeDesc.from(CPointer(CHAR), listOf())),
            FieldMember("reg_save_area", TypeDesc.from(CPointer(CHAR), listOf())))
        )
    }
}