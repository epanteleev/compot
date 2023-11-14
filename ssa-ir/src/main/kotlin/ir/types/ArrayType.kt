package ir.types

data class ArrayType(private val type: Type, val size: Int) : Type {
    override fun ptr(): Type {
        return PointerType(this)
    }

    fun elementType(): Type {
        return type
    }

    override fun size(): Int {
        return size * type.size()
    }

    override fun toString(): String {
        return "<$type x $size>"
    }
}