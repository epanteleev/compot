package ir.types

data class ArrayType(val type: Type, val size: Int) : Type {
    override fun ptr(): Type {
        return PointerType(this)
    }

    override fun size(): Int {
        return size * type.size()
    }

    override fun toString(): String {
        return "<$size x $type>"
    }
}