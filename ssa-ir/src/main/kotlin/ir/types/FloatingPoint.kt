package ir.types

data class FloatingPoint(val size: Int) : PrimitiveType {
    override fun ptr(): Type {
        return PointerType(this)
    }

    override fun size(): Int {
        return size
    }
}