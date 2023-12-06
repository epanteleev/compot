package ir.types

class UndefinedType : Type {
    override fun ptr(): PointerType {
        throw TypeErrorException("cannot do it for undefined type")
    }

    override fun size(): Int {
        throw TypeErrorException("cannot do it for undefined type")
    }
}