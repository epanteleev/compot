package ir.types

data class TypeErrorException(override val message: String) : Exception(message)

interface Type {
    fun ptr(): PointerType {
        return Ptr
    }

    fun size(): Int

    companion object {
        val U1 = BooleanType()

        val U8 = UnsignedIntType(1)
        val U16 = UnsignedIntType(2)
        val U32 = UnsignedIntType(4)
        val U64 = UnsignedIntType(8)

        val I8 = SignedIntType(1)
        val I16 = SignedIntType(2)
        val I32 = SignedIntType(4)
        val I64 = SignedIntType(8)

        val F32 = FloatingPointType(4)
        val F64 = FloatingPointType(8)

        val Ptr = PointerType()

        val Void = VoidType()
        val UNDEF = UndefinedType()
    }
}