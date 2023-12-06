package ir.types

data class FloatingPoint(val size: Int) : PrimitiveType {
    override fun size(): Int {
        return size
    }
}