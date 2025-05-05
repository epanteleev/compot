package ir.value.constant

import ir.types.FlagType


sealed class BoolValue(val bool: Boolean): TrivialConstant {
    override fun type(): FlagType = FlagType

    override fun toString(): String {
        return bool.toString()
    }

    companion object {
        fun of(value: Boolean): BoolValue {
            return if (value) TrueBoolValue else FalseBoolValue
        }
    }
}

object TrueBoolValue: BoolValue(true)
object FalseBoolValue: BoolValue(false)