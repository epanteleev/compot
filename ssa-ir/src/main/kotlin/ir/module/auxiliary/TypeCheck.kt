package ir.module.auxiliary

import ir.instruction.*
import ir.types.*

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
        for (use in phi.usages()) {
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
        val type = load.type()
        if (type is VoidType || type is BooleanType) {
            return false
        }

        val ptrType = load.operand().type()
        if (ptrType !is PointerType) {
            return false
        }

        return load.type() == ptrType.dereference()
    }

    fun checkStore(store: Store): Boolean {
        val type = store.value().type()
        if (type is VoidType || type is BooleanType) {
            return false
        }

        val ptrType = store.pointer().type()
        if (ptrType !is PointerType) {
            return false
        }

        return store.value().type() == ptrType.dereference()
    }
    
    fun checkCall(call: Call): Boolean {
        val returnType = call.type()
        val prototypeReturnType = call.prototype().type()

        for ((expectedType, value) in call.prototype().arguments() zip call.arguments()) {
            if (expectedType != value.type()) {
                return false
            }
        }

        return returnType == prototypeReturnType
    }

    fun checkCast(cast: Cast): Boolean {
        when (cast.castType) {
            CastType.ZeroExtend, CastType.SignExtend, CastType.Truncate -> {
                if (cast.type() !is ArithmeticType || cast.value().type() !is ArithmeticType) {
                    return false
                }

                return if (cast.castType == CastType.Truncate) {
                    cast.type().size() < cast.value().type().size()
                } else {
                    cast.type().size() >= cast.value().type().size()
                }
            }
            CastType.Bitcast -> {
                return cast.type().size() == cast.value().type().size()
            }
        }
    }

    fun checkSelect(select: Select): Boolean {
        if (select.onTrue().type() != select.onFalse().type()) {
            return false
        }

        if (select.condition().type() != Type.U1) {
            return false
        }

        return select.onTrue().type() == select.type()
    }

    fun checkAlloc(alloc: Alloc): Boolean {
        val vType = alloc.type().dereference()
        return !(vType is VoidType || vType is UndefinedType)
    }

    fun checkGep(gep: GetElementPtr): Boolean {
        val index = gep.index()
        val source = gep.source()

        if (index.type() !is ArithmeticType) {
            return false
        }
        if (source.type() !is PointerType) {
            return false
        }

        val sourceKind = source.type()
        return sourceKind != Type.Void && sourceKind != Type.U1
    }
}