package ir.types

data class FloatingPoint(val size: Int) : PrimitiveType {
    override fun size(): Int {
        return size
    }

    override fun toString(): String {
        return when (size) {
            4 -> "f32"
            8 -> "f64"
            else -> throw TypeErrorException("unsupported size=$size")
        }
    }
}