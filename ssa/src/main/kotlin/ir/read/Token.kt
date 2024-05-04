package ir.read

import ir.types.*
import ir.read.bulder.TypeResolver


interface AnyToken {
    fun message(): String
}

sealed class Token(protected open val line: Int, protected open val pos: Int): AnyToken {
    fun position(): String {
        return "$line:$pos"
    }
}

data class Identifier(val string: String, override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "identifier '$string'"
    }
}

abstract class AnyValueToken(override val line: Int, override val pos: Int): Token(line, pos)

data class IntValue(val int: Long, override val line: Int, override val pos: Int): AnyValueToken(line, pos) {
    override fun message(): String {
        return "int value '$int'"
    }
}

data class FloatValue(val fp: Double, override val line: Int, override val pos: Int): AnyValueToken(line, pos) {
    override fun message(): String {
        return "float value '$fp'"
    }
}

abstract class ValueToken(override val line: Int, override val pos: Int): AnyValueToken(line, pos)

data class LocalValueToken(val name: String, override val line: Int, override val pos: Int): ValueToken(line, pos) {
    override fun message(): String {
        return "value '%$name'"
    }
}

abstract class TypeToken(override val line: Int, override val pos: Int) : Token(line, pos) {
    abstract fun type(resolver: TypeResolver): Type
}

interface ArithmeticTypeToken: AnyToken {
    fun type(resolver: TypeResolver): Type
}

abstract class PrimitiveTypeToken(protected open val type: PrimitiveType, override val line: Int, override val pos: Int) : TypeToken(line, pos) {
    override fun type(resolver: TypeResolver): Type {
        return type
    }

    abstract fun type(): PrimitiveType

    inline fun<reified T: Type> asType(): T {
        val ty = type()
        if (ty !is T) {
            throw RuntimeException("actual type=$ty")
        }

        return ty
    }

    override fun message(): String {
        return "type '$type'"
    }
}

abstract class IntegerTypeToken(override val type: IntegerType, override val line: Int, override val pos: Int)
    : PrimitiveTypeToken(type, line, pos), ArithmeticTypeToken {
    abstract override fun type(): IntegerType
}

data class SignedIntegerTypeToken(override val type: SignedIntType, override val line: Int, override val pos: Int)
    : IntegerTypeToken(type, line, pos), ArithmeticTypeToken {
    override fun type(): SignedIntType =  type
}

data class UnsignedIntegerTypeToken(override val type: UnsignedIntType, override val line: Int, override val pos: Int)
    : IntegerTypeToken(type, line, pos), ArithmeticTypeToken {
    override fun type(): UnsignedIntType =  type
}

data class FloatTypeToken(override val type: FloatingPointType, override val line: Int, override val pos: Int)
    : PrimitiveTypeToken(type, line, pos), ArithmeticTypeToken {
    override fun type(): FloatingPointType = type
}

data class PointerTypeToken(override val line: Int, override val pos: Int)
    : PrimitiveTypeToken(Type.Ptr, line, pos)  {
    override fun type(): PointerType = Type.Ptr
}

data class BooleanTypeToken(override val line: Int, override val pos: Int)
    : PrimitiveTypeToken(Type.U1, line, pos)  {
    override fun type(): BooleanType = Type.U1
}

data class VoidTypeToken(override val line: Int, override val pos: Int) : TypeToken(line, pos) {
    override fun type(resolver: TypeResolver): Type {
        return Type.Void
    }

    override fun message(): String = "type '${Type.Void}'"
    fun type(): Type = Type.Void
}

abstract class AggregateTypeToken(override val line: Int, override val pos: Int) : TypeToken(line, pos) {
    abstract override fun type(resolver: TypeResolver): AggregateType
}

data class ArrayTypeToken(val size: Long, val type: TypeToken, override val line: Int, override val pos: Int) : AggregateTypeToken(line, pos) {
    override fun type(resolver: TypeResolver): ArrayType {
        val tp = type.type(resolver)
        if (tp !is NonTrivialType) {
            throw TypeErrorException("expect non-trivial type as array element, but type=${type}")
        }

        return ArrayType(tp, size.toInt())
    }

    override fun message(): String {
        return "<${type.message()}, $size>"
    }
}

data class StructDefinition(val name: String, override val line: Int, override val pos: Int): AggregateTypeToken(line, pos) {
    override fun type(resolver: TypeResolver): StructType {
        return resolver.resolve(name)
    }

    override fun message(): String {
        return "$$name"
    }
}

data class OpenParen(override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "'('"
    }
}

data class CloseParen(override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "')'"
    }
}

data class OpenBrace(override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "'{'"
    }
}

data class CloseBrace(override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "'}'"
    }
}

data class Equal(override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "'='"
    }
}

data class ConstantKeyword(override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "'constant'"
    }
}

data class GlobalKeyword(override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "'global'"
    }
}

data class Comma(override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "','"
    }
}

data class Dot(override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "'.'"
    }
}

data class Define(override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "'define'"
    }
}

data class To(override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "'to'"
    }
}

data class TypeKeyword(override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "'type'"
    }
}

data class LabelUsage(val labelName: String, override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "'label'"
    }
}

data class Colon(override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "':'"
    }
}

data class Extern(override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "'extern'"
    }
}

data class LabelDefinition(val name: String, override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "label '$name:'"
    }
}

data class OpenSquareBracket(override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "'['"
    }
}

data class CloseSquareBracket(override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "']'"
    }
}

data class OpenTriangleBracket(override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "'<'"
    }
}

data class CloseTriangleBracket(override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "'>'"
    }
}

data class SymbolValue(val name: String, override val line: Int, override val pos: Int): ValueToken(line, pos) {
    override fun message(): String {
        return "@$name"
    }
}

data class StringLiteralToken(val string: String, override val line: Int, override val pos: Int): Token(line, pos) {
    override fun message(): String {
        return "\"$string\""
    }
}