package ir.types

data class IntType(val size: Int) : PrimitiveType, ArithmeticType {
    override fun ptr(): Type {
        return PointerType(this)
    }

    override fun size(): Int {
        return size
    }

    override fun toString(): String {
        return when (size) {
            1 -> "i8"
            2 -> "i16"
            4 -> "i32"
            8 -> "i64"
            else -> throw TypeErrorException("unsupported size=$size")
        }
    }
}

data class UIntType(val size: Int) : PrimitiveType, ArithmeticType {
    override fun ptr(): Type {
        return PointerType(this)
    }

    override fun size(): Int {
        return size
    }

    override fun toString(): String {
        return when (size) {
            1 -> "u8"
            2 -> "u16"
            4 -> "u32"
            8 -> "u64"
            else -> throw TypeErrorException("unsupported size=$size")
        }
    }
}