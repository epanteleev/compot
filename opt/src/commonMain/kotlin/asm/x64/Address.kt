package asm.x64


sealed interface Address : Operand {
    companion object {
        fun from(base: GPRegister, offset: Int): Address {
            return Address2(base, offset)
        }

        fun from(base: GPRegister?, offset: Int, index: GPRegister, scale: Int): Address {
            return Address4(base, offset, index, scale)
        }

        fun internal(label: String): Address {
            return InternalAddressLiteral(0, label)
        }

        fun external(label: String): Address {
            return ExternalAddressLiteral(label)
        }
    }

    fun withOffset(disp: Int): Address
}

class Address2 internal constructor(val base: GPRegister, val offset: Int) : Address {
    override fun withOffset(disp: Int): Address {
        return Address2(base, offset + disp)
    }

    override fun toString(): String {
        return toString(Int.MAX_VALUE)
    }

    override fun toString(size: Int): String {
        return if (offset == 0) {
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
        if (other == null || this::class != other::class) return false

        other as Address2

        if (base != other.base) return false
        if (offset != other.offset) return false
        return true
    }
}

class Address4 internal constructor(private val base: GPRegister?, private val offset: Int, val index: GPRegister, private val scale: Int) :
    Address {
    override fun withOffset(disp: Int): Address {
        return Address4(base, offset + (disp * scale), index, scale)
    }

    override fun toString(): String {
        return toString(Int.MAX_VALUE)
    }

    private fun base(size: Int): String {
        return base?.toString(size) ?: ""
    }

    override fun toString(size: Int): String {
        return if (offset == 0) {
            if (scale == 1) {
                "(${base(8)}, ${index.toString(8)})"
            } else {
                "(${base(8)}, ${index.toString(8)}, $scale)"
            }
        } else {
            if (scale == 1) {
                "$offset(${base(8)}, ${index.toString(8)})"
            } else {
                "$offset(${base(8)}, ${index.toString(8)}, $scale)"
            }
        }
    }
}

class ArgumentSlot(private val base: GPRegister, private val offset: Int) : Address {
    override fun withOffset(disp: Int): Address {
        return ArgumentSlot(base, offset + disp)
    }

    override fun toString(): String {
        return toString(Int.MAX_VALUE)
    }

    override fun toString(size: Int): String {
        return if (offset == 0) {
            "(${base.toString(8)})"
        } else {
            "$offset(${base.toString(8)})"
        }
    }
}

abstract class AddressLiteral internal constructor(val label: String) : Address

class InternalAddressLiteral internal constructor(private val offset: Int, label: String) : AddressLiteral(label) {
    override fun withOffset(disp: Int): Address = InternalAddressLiteral(offset + disp, label)
    override fun toString(): String = if (offset != 0) {
        "$label+$offset(%rip)"
    } else {
        "$label(%rip)"
    }

    override fun toString(size: Int): String {
        return toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as InternalAddressLiteral

        if (label != other.label) return false

        return true
    }

    override fun hashCode(): Int {
        return label.hashCode()
    }
}

class ExternalAddressLiteral internal constructor(label: String) : AddressLiteral(label) {
    override fun withOffset(disp: Int): Address = throw RuntimeException("impossible to calculate")
    override fun toString(): String {
        return "$label@GOTPCREL(%rip)"
    }

    override fun toString(size: Int): String {
        return toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ExternalAddressLiteral

        if (label != other.label) return false

        return true
    }

    override fun hashCode(): Int {
        return label.hashCode()
    }
}