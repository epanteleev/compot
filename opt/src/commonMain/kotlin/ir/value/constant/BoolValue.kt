package ir.value.constant

import ir.types.Type


class BoolValue private constructor(val bool: Boolean): Constant {
    override fun type(): Type {
        return Type.U1
    }

    override fun toString(): String {
        return bool.toString()
    }

    companion object {
        val TRUE = BoolValue(true)
        val FALSE = BoolValue(false)

        fun of(value: Boolean): BoolValue {
            return if (value) TRUE else FALSE
        }
    }
}