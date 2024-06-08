package common

import java.util.*

fun<T> identityHashSetOf(): MutableSet<T> {
    return Collections.newSetFromMap<T>(IdentityHashMap()) //TODO Java backend only
}