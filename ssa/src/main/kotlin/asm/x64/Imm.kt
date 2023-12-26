package asm.x64

interface Imm: AnyOperand {
    companion object {
        private val minusZeroFloat = ImmInt(0x80000000)
        private val minusZeroDouble = ImmInt((-0.0).toBits())

        fun minusZero(size: Int): ImmInt {
            return when (size) {
                4 -> minusZeroFloat
                8 -> minusZeroDouble
                else -> throw RuntimeException("size=$size")
            }
        }
    }
}