package types

import tokenizer.Position


sealed class CAggregateType: CompletedType() {
    fun hasFloatOnly(lo: Int, hi: Int): Boolean {
        return hasFloat(this, lo, hi, 0)
    }

    private fun hasFloat(ty: CType, lo: Int, hi: Int, offset: Int): Boolean {
        if (ty is AnyCStructType) {
            for ((idx, field) in ty.members().withIndex()) {
                if (!hasFloat(field.cType(), lo, hi, offset + ty.offset(idx))) {
                    return false
                }
            }
            return true

        } else if (ty is CArrayType) {
            val elementType = ty.type.asType<CompletedType>(Position.UNKNOWN)
            for (i in 0 until ty.dimension.toInt()) {
                if (!hasFloat(ty.type.cType(), lo, hi, offset + i * elementType.size())) {
                    return false
                }
            }
            return true
        }

        return offset < lo || hi <= offset || ty is FLOAT || ty is DOUBLE
    }
}