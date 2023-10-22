package ir.types

data class PointerType(val type: Type) : PrimitiveType {
    override fun ptr(): PointerType {
        return PointerType(this)
    }

    override fun size(): Int {
        return 8
    }

    override fun toString(): String {
        return "$type*"
    }

    fun dereference(): Type {
        return type
    }

    inline fun <reified T : Type> asDereference(): T {
        val ty = dereference()
        if (ty !is T) {
            throw TypeErrorException("actual type=$ty")
        }

        return ty
    }

    companion object {
        fun of(type: Type, indirections: Int): PointerType {
            require(indirections >= 1) {
                "indirections=$indirections"
            }

            var pointer = PointerType(type)
            for (i in 0..<(indirections - 1)) {
                pointer = PointerType(pointer)
            }

            return pointer
        }
    }
}