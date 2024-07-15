package ir.types


data class FloatingPointType internal constructor(private val size: Int) : ArithmeticType {
    override fun sizeOf(): Int {
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