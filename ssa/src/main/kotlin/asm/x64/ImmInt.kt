package asm.x64

sealed interface ImmInt: Imm {
    fun value(): Long

    companion object {
        val minusZeroFloat: Long = 0x80000000
        val minusZeroDouble: Long = (-0.0).toBits()
    }
}

data class Imm32(val value: Long) : ImmInt {
    init {
        require(Int.MIN_VALUE < value && value < Int.MAX_VALUE) //TODO
    }

    override fun toString(): String {
        return "$$value"
    }

    override fun value(): Long = value

    override fun toString(size: Int): String {
        return toString()
    }
}

data class Imm64(val value: Long) : ImmInt {
    override fun toString(): String {
        return "$$value"
    }

    override fun value(): Long = value

    override fun toString(size: Int): String {
        return toString()
    }
}