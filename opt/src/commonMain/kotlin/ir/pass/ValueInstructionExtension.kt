package ir.pass

import ir.instruction.*


fun Alloc.isLocalVariable(): Boolean {
    return noEscape()
}

fun Load.isLocalVariable(): Boolean {
    return canBeReplaced() && (operand() as Alloc).isLocalVariable()
}

fun Load.canBeReplaced(): Boolean {
    val operand = operand()
    return operand is Alloc
}

fun Store.isLocalVariable(): Boolean {
    val pointer = pointer()
    return (pointer is Alloc && pointer.isLocalVariable())
}

/** Check whether alloc result isn't leak to other function. **/
fun Alloc.noEscape(): Boolean {
    for (user in usedIn()) {
        if (user is Load) {
            continue
        }

        if (user is Store && user.pointer() == this) {
            continue
        }
        return false
    }
    return true
}