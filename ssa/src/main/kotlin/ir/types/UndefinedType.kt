package ir.types

class UndefinedType : Type {
    override fun size(): Int {
        throw TypeErrorException("cannot do it for undefined type")
    }
}