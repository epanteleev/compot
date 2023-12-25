package ir.platform.x64.utils

import asm.x64.AnyOperand

object Utils {
    inline fun <reified F: AnyOperand, reified S: AnyOperand, reified T: AnyOperand>
            case(first: AnyOperand, second: AnyOperand, third: AnyOperand): Boolean {
        return first is F && second is S && third is T
    }

    inline fun <reified F: AnyOperand, reified S: AnyOperand>
            case(first: AnyOperand, second: AnyOperand): Boolean {
        return first is F && second is S
    }
}