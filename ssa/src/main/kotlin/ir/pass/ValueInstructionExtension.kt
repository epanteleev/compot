package ir.pass

import ir.instruction.*
import ir.types.PrimitiveType


internal object ValueInstructionExtension {
    fun Alloc.isLocalVariable(): Boolean {
        return allocatedType is PrimitiveType
    }

    fun Load.isLocalVariable(): Boolean {
        val operand = operand()
        if (operand is Generate) {
            return true
        }

        return operand is Alloc && operand.allocatedType is PrimitiveType
    }

    fun Store.isLocalVariable(): Boolean {
        val pointer = pointer()
        return (pointer is Alloc && pointer.isLocalVariable()) || pointer is Generate
    }
}