package asm.x64


interface Address : Operand {
    companion object {
        fun mem(base: GPRegister, offset: Long): Address {
            return Address2(base, offset)
        }

        fun mem(base: GPRegister?, offset: Long, index: GPRegister, disp: Long): Address {
            return Address4(base, offset, index, disp)
        }

        fun mem(label: String): Address {
            return AddressLiteral(label)
        }
    }
}

class Address2 internal constructor(val base: GPRegister, val offset: Long) : Address {
    override fun toString(): String {
        return if (offset == 0L) {
            "($base)"
        } else {
            "$offset($base)"
        }
    }

    override fun toString(size: Int): String {
        return if (offset == 0L) {
            "(${base.toString(8)})"
        } else {
            "$offset(${base.toString(8)})"
        }
    }

    override fun hashCode(): Int {
        return base.hashCode() xor offset.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Address2

        if (base != other.base) return false
        if (offset != other.offset) return false
        return true
    }
}

class Address4 internal constructor(private val base: GPRegister?, private val offset: Long, val index: GPRegister, val disp: Long) :
    Address {
    override fun toString(): String {
        return if (offset == 0L) {
            "($base, $index, $disp)"
        } else {
            "$offset($base, $index, $disp)"
        }
    }

    private fun base(size: Int): String {
        return base?.toString(size) ?: ""
    }

    override fun toString(size: Int): String {
        return if (offset == 0L) {
            "(${base(8)}, ${index.toString(8)}, $disp)"
        } else {
            "$offset(${base(8)}, ${index.toString(8)}, $disp)"
        }
    }
}

class ArgumentSlot(private val base: GPRegister, private val offset: Long) : Address {
    override fun toString(size: Int): String {
        return if (offset == 0L) {
            "(${base.toString(8)})"
        } else {
            "$offset(${base.toString(8)})"
        }
    }
}

data class AddressLiteral internal constructor(val label: String) : Address {
    override fun toString(): String {
        return "$label(%rip)"
    }

    override fun toString(size: Int): String {
        return toString()
    }
}