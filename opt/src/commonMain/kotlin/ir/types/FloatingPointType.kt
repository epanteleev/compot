package ir.types

import ir.Definitions.DOUBLE_SIZE
import ir.Definitions.FLOAT_SIZE


sealed class FloatingPointType(private val size: Int) : ArithmeticType {
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

object F32Type: FloatingPointType(FLOAT_SIZE)
object F64Type: FloatingPointType(DOUBLE_SIZE)