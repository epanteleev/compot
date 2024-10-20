package asm.x64

import asm.Operand
import ir.Definitions.QWORD_SIZE


sealed interface Address : Operand {
    companion object {
        fun from(base: GPRegister, offset: Int): Address {
            return Address2(base, offset)
        }

        fun from(base: GPRegister?, offset: Int, index: GPRegister, scale: ScaleFactor): Address {
            return Address4(base, offset, index, scale)
        }

        fun internal(label: String): Address {
            return InternalAddressLiteral(0, label)
        }

        fun external(label: String): Address {
            return ExternalAddressLiteral(label)
        }
    }
}

class Address2 internal constructor(val base: GPRegister, val offset: Int) : Address {
    fun withOffset(index: GPRegister): Address {
        return Address4(base, offset, index, ScaleFactor.TIMES_1)
    }

    fun withOffset(disp: Int): Address {
        return Address2(base, offset + disp)
    }

    override fun toString(): String {
        return toString(Int.MAX_VALUE)
    }

    override fun toString(size: Int): String {
        return if (offset == 0) {
            "(${base.toString(QWORD_SIZE)})"
        } else {
            "$offset(${base.toString(QWORD_SIZE)})"
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

class Address4 internal constructor(private val base: GPRegister?, private val offset: Int, val index: GPRegister, private val scale: ScaleFactor) :
    Address {
    override fun toString(): String {
        return toString(Int.MAX_VALUE)
    }

    private fun base(size: Int): String {
        return base?.toString(size) ?: ""
    }

    override fun toString(size: Int): String {
        return if (offset == 0) {
            if (scale == ScaleFactor.TIMES_1) {
                "(${base(QWORD_SIZE)}, ${index.toString(QWORD_SIZE)})"
            } else {
                "(${base(QWORD_SIZE)}, ${index.toString(QWORD_SIZE)}, $scale)"
            }
        } else {
            if (scale == ScaleFactor.TIMES_1) {
                "$offset(${base(QWORD_SIZE)}, ${index.toString(QWORD_SIZE)})"
            } else {
                "$offset(${base(QWORD_SIZE)}, ${index.toString(QWORD_SIZE)}, $scale)"
            }
        }
    }
}

class ArgumentSlot(private val base: GPRegister, private val offset: Int) : Address {
    override fun toString(): String {
        return toString(Int.MAX_VALUE)
    }

    override fun toString(size: Int): String {
        return if (offset == 0) {
            "(${base.toString(QWORD_SIZE)})"
        } else {
            "$offset(${base.toString(QWORD_SIZE)})"
        }
    }
}

abstract class AddressLiteral internal constructor(val label: String) : Address

class InternalAddressLiteral internal constructor(private val offset: Int, label: String) : AddressLiteral(label) {
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

        return label == other.label
    }

    override fun hashCode(): Int {
        return label.hashCode()
    }
}

class ExternalAddressLiteral internal constructor(label: String) : AddressLiteral(label) {
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

        return label == other.label
    }

    override fun hashCode(): Int {
        return label.hashCode()
    }
}