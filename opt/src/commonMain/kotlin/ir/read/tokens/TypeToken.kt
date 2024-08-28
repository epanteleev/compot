package ir.read.tokens

import common.arrayFrom
import ir.read.bulder.TypeResolver
import ir.types.*


abstract class TypeToken(override val line: Int, override val pos: Int) : Token(line, pos) {
    abstract fun type(resolver: TypeResolver): Type

    inline fun<reified T: Type> asType(resolver: TypeResolver): T {
        val ty = type(resolver)
        if (ty !is T) {
            throw RuntimeException("actual type=$ty")
        }

        return ty
    }
}

abstract class PrimitiveTypeToken(protected open val type: PrimitiveType, override val line: Int, override val pos: Int) : TypeToken(line, pos) {
    override fun type(resolver: TypeResolver): NonTrivialType {
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

data class PointerTypeToken(override val line: Int, override val pos: Int)
    : PrimitiveTypeToken(Type.Ptr, line, pos)  {
    override fun type(): PointerType = Type.Ptr
}

data class BooleanTypeToken(override val line: Int, override val pos: Int)
    : PrimitiveTypeToken(Type.U1, line, pos)  {
    override fun type(): BooleanType = Type.U1
}

interface ArithmeticTypeToken: AnyToken {
    fun type(resolver: TypeResolver): NonTrivialType
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

data class VoidTypeToken(override val line: Int, override val pos: Int) : TypeToken(line, pos) {
    override fun type(resolver: TypeResolver): Type {
        return Type.Void
    }

    override fun message(): String = "type '${Type.Void}'"
    fun type(): Type = Type.Void
}

data class TupleTypeToken(val types: List<TypeToken>, override val line: Int, override val pos: Int) : TypeToken(line, pos) {
    override fun type(resolver: TypeResolver): TupleType {
        val types = arrayFrom(types) { it.asType<NonTrivialType>(resolver) }
        return TupleType(types)
    }

    override fun message(): String {
        return "|${types.joinToString()}|"
    }
}

data class Vararg(override val line: Int, override val pos: Int): TypeToken(line, pos) {
    override fun message(): String = "'...'"

    override fun type(resolver: TypeResolver): Type {
        throw TypeErrorException("vararg type is not a type")
    }
}