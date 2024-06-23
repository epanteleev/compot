package ir.instruction

import common.forEachWith
import ir.Value
import ir.types.Type
import ir.module.AnyFunctionPrototype
import ir.module.block.Block


interface Callable {
    fun arguments(): Array<Value>
    fun prototype(): AnyFunctionPrototype
    fun shortName(): String {
        return prototype().shortName()
    }

    fun target(): Block

    fun type(): Type

    companion object {
        internal fun isAppropriateTypes(func: AnyFunctionPrototype, args: Array<Value>): Boolean {
            func.arguments().forEachWith(args) { expectedType, value ->
                if (expectedType == Type.VarArgType) {
                    return true
                }
                if (expectedType != value.type()) {
                    return false
                }
            }

            return true
        }

        internal fun isAppropriateTypes(func: AnyFunctionPrototype, pointer: Value, args: Array<Value>): Boolean {
            if (pointer.type() != Type.Ptr) {
                return false
            }

            return isAppropriateTypes(func, args)
        }

        fun typeCheck(call: Callable): Boolean {
            return when (call) {
                is IndirectionVoidCall -> isAppropriateTypes(call.prototype(), call.pointer(), call.arguments())
                is IndirectionCall     -> isAppropriateTypes(call.prototype(), call.pointer(), call.arguments())
                else -> isAppropriateTypes(call.prototype(), call.arguments())
            }
        }
    }
}