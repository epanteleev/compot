package ir.value.constant

import ir.types.FlagType


sealed class BoolValue: TrivialConstant {
    override fun type(): FlagType = FlagType

    companion object {
        fun of(value: Boolean): BoolValue {
            return if (value) TrueBoolValue else FalseBoolValue
        }
    }
}

object TrueBoolValue: BoolValue() {
    override fun toString(): String = "true"
}
object FalseBoolValue: BoolValue() {
    override fun toString(): String = "false"
}