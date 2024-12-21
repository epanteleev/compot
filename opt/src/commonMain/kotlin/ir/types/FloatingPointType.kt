package ir.types

import ir.Definitions.DOUBLE_SIZE
import ir.Definitions.FLOAT_SIZE


sealed class FloatingPointType : ArithmeticType()

object F32Type: FloatingPointType() {
    override fun sizeOf(): Int = FLOAT_SIZE
    override fun toString(): String = "f32"
}
object F64Type: FloatingPointType() {
    override fun sizeOf(): Int = DOUBLE_SIZE
    override fun toString(): String = "f64"
}