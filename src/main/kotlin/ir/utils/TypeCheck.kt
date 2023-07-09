package ir.utils

import ir.*

object TypeCheck {
    fun checkUnary(unary: ArithmeticUnary): Boolean {
        return unary.type() == unary.operand().type()
    }

    fun checkBinary(binary: ArithmeticBinary): Boolean {
        val type = binary.type()
        return (type == binary.first().type()) &&
                (type == binary.second().type())
    }

    fun checkPhi(phi: Phi): Boolean {
        val type = phi.type()
        for (use in phi.usages) {
            if (type != use.type()) {
                return false
            }
        }
        return true
    }

    fun checkIntCompare(cmp: IntCompare): Boolean {
        return cmp.first().type() == cmp.second().type()
    }

    fun checkLoad(load: Load): Boolean {
        val kind = load.type().kind
        if (kind == TypeKind.VOID || kind == TypeKind.U1) {
            return false
        }

        if (!load.operand().type().isPointer()) {
            return false
        }

        return load.type() == load.operand().type().dereference()
    }

    fun checkStore(load: Store): Boolean {
        val kind = load.value().type().kind
        if (kind == TypeKind.VOID || kind == TypeKind.U1) {
            return false
        }

        if (!load.pointer().type().isPointer()) {
            return false
        }

        return load.value().type() == load.pointer().type().dereference()
    }
    
    fun checkCall(call: Call): Boolean {
        TODO()
    }

    fun checkCast(call: Cast): Boolean {
        TODO()
    }

    fun checkSelect(call: Select): Boolean {
        TODO()
    }
}