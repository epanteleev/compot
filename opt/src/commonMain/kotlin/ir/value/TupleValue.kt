package ir.value

import ir.types.TupleType
import ir.instruction.Projection


interface TupleValue: LocalValue {
    fun proj(index: Int): Projection? {
        if (type() !is TupleType) {
            throw IllegalStateException("Cannot project from non-tuple value")
        }
        for (user in usedIn()) {
            user as Projection
            if (user.index() == index) {
                return user
            }
        }
        return null
    }
}