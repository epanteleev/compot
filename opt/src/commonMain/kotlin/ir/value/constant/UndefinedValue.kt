package ir.value.constant

import ir.types.*


object UndefinedValue: Constant, PrimitiveConstant {
    fun name(): String {
        return toString()
    }

    override fun data(): String = toString()

    override fun type(): UndefType {
        return Type.UNDEF
    }

    override fun toString(): String {
        return "undef"
    }

    override fun hashCode(): Int {
        return -1;
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        return true
    }
}