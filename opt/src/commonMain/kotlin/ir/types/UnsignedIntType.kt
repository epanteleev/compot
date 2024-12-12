package ir.types

data class UnsignedIntType(private val size: Int) : IntegerType {
    override fun sizeOf(): Int {
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