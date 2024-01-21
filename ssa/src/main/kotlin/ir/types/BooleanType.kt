package ir.types

class BooleanType : PrimitiveType {
    override fun toString(): String = "u1"
    override fun size(): Int = 1
}