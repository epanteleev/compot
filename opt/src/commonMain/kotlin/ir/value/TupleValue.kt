package ir.value

import ir.types.TupleType
import ir.instruction.Projection


interface TupleValue: LocalValue {
    override fun type(): TupleType

    fun proj(index: Int): Projection? {
        for (user in usedIn()) {
            user as Projection
            if (user.index() == index) {
                return user
            }
        }
        return null
    }

    fun proj(visitor: (Projection) -> Unit) {
        for (user in usedIn()) {
            user as Projection
            visitor(user)
        }
    }
}