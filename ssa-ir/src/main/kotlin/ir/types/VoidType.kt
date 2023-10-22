package ir.types

class VoidType : Type {
    override fun ptr(): Type {
        throw TypeErrorException("cannot do it for void type")
    }

    override fun size(): Int {
        throw TypeErrorException("cannot do it for void type")
    }
}