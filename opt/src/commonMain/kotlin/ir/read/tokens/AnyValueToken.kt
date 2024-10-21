package ir.read.tokens

import ir.types.NonTrivialType
import ir.types.PrimitiveType
import ir.value.constant.NullValue
import ir.value.constant.NonTrivialConstant
import ir.value.constant.PrimitiveConstant


abstract class AnyValueToken(override val line: Int, override val pos: Int): Token(line, pos)

abstract class LiteralValueToken(override val line: Int, override val pos: Int): AnyValueToken(line, pos) {
    fun toConstant(ty: PrimitiveType): NonTrivialConstant = when (this) {
        is IntValue       -> PrimitiveConstant.of(ty, int)
        is FloatValue     -> PrimitiveConstant.of(ty, fp)
        is NULLValueToken -> NullValue.NULLPTR
        else -> throw RuntimeException("unexpected literal value: $this")
    }
}

data class IntValue(val int: Long, override val line: Int, override val pos: Int): LiteralValueToken(line, pos) {
    override fun message(): String = "int value '$int'"
}

data class FloatValue(val fp: Double, override val line: Int, override val pos: Int): LiteralValueToken(line, pos) {
    override fun message(): String = "float value '$fp'"
}

data class BoolValueToken(val bool: Boolean, override val line: Int, override val pos: Int): LiteralValueToken(line, pos) {
    override fun message(): String = "bool value '$bool'"
}

data class NULLValueToken(override val line: Int, override val pos: Int): LiteralValueToken(line, pos) {
    override fun message(): String = "null"
}

abstract class ValueToken(override val line: Int, override val pos: Int): AnyValueToken(line, pos)

data class LocalValueToken(val name: String, override val line: Int, override val pos: Int): ValueToken(line, pos) {
    override fun message(): String = "value '%$name'"

    fun value(): String = "%${name}"
}

data class SymbolValue(val name: String, override val line: Int, override val pos: Int): ValueToken(line, pos) {
    override fun message(): String = "@$name"
}