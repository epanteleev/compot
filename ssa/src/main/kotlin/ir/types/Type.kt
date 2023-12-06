package ir.types

data class TypeErrorException(override val message: String) : Exception(message)

interface Type {
    fun ptr(): PointerType {
        return Ptr
    }

    fun size(): Int

    companion object {
        val U1 = BooleanType()

        val U8 = UIntType(1)
        val U16 = UIntType(2)
        val U32 = UIntType(4)
        val U64 = UIntType(8)

        val I8 = IntType(1)
        val I16 = IntType(2)
        val I32 = IntType(4)
        val I64 = IntType(8)

        val F32 = FloatingPoint(4)
        val F64 = FloatingPoint(8)

        val Ptr = PointerType()

        val Void = VoidType()
        val UNDEF = UndefinedType()
    }
}