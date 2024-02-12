package ir.pass

import ir.instruction.*
import ir.types.PrimitiveType


fun Alloc.canBeReplaced(): Boolean {
    return allocatedType is PrimitiveType
}

fun Alloc.isLocalVariable(): Boolean {
    return canBeReplaced() && noEscape()
}

fun Load.isLocalVariable(): Boolean {
    return canBeReplaced() && (operand() as Alloc).isLocalVariable()
}

fun Load.canBeReplaced(): Boolean {
    val operand = operand()
    if (operand is Generate) {
        return true
    }

    return operand is Alloc
}

fun Store.isLocalVariable(): Boolean {
    val pointer = pointer()
    return (pointer is Alloc && pointer.isLocalVariable()) || pointer is Generate
}

fun Store.canBeReplaced(): Boolean {
    val pointer = pointer()
    return pointer is Alloc || pointer is Generate
}

/** Check whether alloc result isn't leak to other function. **/
fun Alloc.noEscape(): Boolean {
    return usedIn().fold(true) { acc, value ->
        acc && (value is Load || value is Store)
    }
}