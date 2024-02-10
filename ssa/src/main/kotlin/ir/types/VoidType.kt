package ir.types

class VoidType : Type {
    override fun size(): Int {
        throw TypeErrorException("cannot do it for void type")
    }

    override fun toString(): String {
        return "void"
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }
}