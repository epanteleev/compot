package ir.types

import ir.Definitions.BYTE_SIZE
import ir.Definitions.WORD_SIZE
import ir.Definitions.FLOAT_SIZE
import ir.Definitions.HWORD_SIZE
import ir.Definitions.QWORD_SIZE
import ir.Definitions.DOUBLE_SIZE


data class TypeErrorException(override val message: String) : Exception(message)

sealed interface Type {
    companion object {
        val U1  = FlagType

        val U8  = UnsignedIntType(BYTE_SIZE)
        val U16 = UnsignedIntType(HWORD_SIZE)
        val U32 = UnsignedIntType(WORD_SIZE)
        val U64 = UnsignedIntType(QWORD_SIZE)

        val I8  = SignedIntType(BYTE_SIZE)
        val I16 = SignedIntType(HWORD_SIZE)
        val I32 = SignedIntType(WORD_SIZE)
        val I64 = SignedIntType(QWORD_SIZE)

        val F32 = FloatingPointType(FLOAT_SIZE)
        val F64 = FloatingPointType(DOUBLE_SIZE)

        val Ptr = PointerType

        val Void  = VoidType
        val UNDEF = UndefType
    }
}

inline fun<reified T: Type> Type.asType(): T {
    if (this !is T) {
        throw TypeErrorException("Type $this is not a ${T::class.simpleName}")
    }

    return this
}