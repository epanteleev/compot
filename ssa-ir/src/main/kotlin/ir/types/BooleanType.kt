package ir.types

class BooleanType : Type {
    override fun ptr(): Type {
        return PointerType(this)
    }

    override fun size(): Int {
        return 1
    }
}