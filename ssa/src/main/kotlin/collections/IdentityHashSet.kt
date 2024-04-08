package collections

import java.util.*
import java.util.IdentityHashMap

fun<T> identityHashSetOf(): MutableSet<T> {
    return Collections.newSetFromMap<T>(IdentityHashMap()) //TODO Java backend only
}