package ir.types

sealed interface AggregateType : NonTrivialType {
    fun offset(index: Int): Int
    fun field(index: Int): NonTrivialType
    fun fields(): List<NonTrivialType>

    fun hasFloatOnly(lo: Int, hi: Int): Boolean {
        return hasFloat(this, lo, hi, 0)
    }

    private fun hasFloat(ty: NonTrivialType, lo: Int, hi: Int, offset: Int): Boolean {
        if (ty is StructType) {
            for ((idx, field) in ty.fields.withIndex()) {
                if (!hasFloat(field, lo, hi, offset + ty.offset(idx))) { //TODO inefficient
                    return false
                }
            }
            return true

        } else if (ty is ArrayType) {
            for (i in 0 until ty.length) {
                if (!hasFloat(ty.elementType(), lo, hi, offset + i * ty.elementType().sizeOf())) {
                    return false
                }
            }
            return true
        }

        return offset < lo || hi <= offset || ty is FloatingPointType
    }
}