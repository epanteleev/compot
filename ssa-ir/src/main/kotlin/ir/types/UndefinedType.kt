package ir.types

class UndefinedType : Type {
    override fun ptr(): Type {
        throw TypeErrorException("cannot do it for undefined type")
    }

    override fun size(): Int {
        throw TypeErrorException("cannot do it for undefined type")
    }
}