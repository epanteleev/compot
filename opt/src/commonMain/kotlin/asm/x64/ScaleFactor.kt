package asm.x64

enum class ScaleFactor {
    TIMES_1,
    TIMES_2,
    TIMES_4,
    TIMES_8;

    override fun toString(): String {
        return when (this) {
            TIMES_1 -> "1"
            TIMES_2 -> "2"
            TIMES_4 -> "4"
            TIMES_8 -> "8"
        }
    }

    fun value(): Int {
        return when (this) {
            TIMES_1 -> 1
            TIMES_2 -> 2
            TIMES_4 -> 4
            TIMES_8 -> 8
        }
    }

    companion object {
        fun from(value: Int): ScaleFactor = when (value) {
            1 -> TIMES_1
            2 -> TIMES_2
            4 -> TIMES_4
            8 -> TIMES_8
            else -> throw IllegalArgumentException("Invalid scale factor: $value")
        }

        fun isScaleFactor(scale: Int): Boolean {
            return scale == 1 || scale == 2 || scale == 4 || scale == 8
        }
    }
}