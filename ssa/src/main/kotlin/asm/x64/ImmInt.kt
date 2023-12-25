package asm.x64

data class ImmInt(val value: Long) : Imm {
    override fun toString(): String {
        return "$$value"
    }

    override fun toString(size: Int): String {
        return toString()
    }
}