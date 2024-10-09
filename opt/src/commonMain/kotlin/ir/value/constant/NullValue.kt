package ir.value.constant

import ir.types.*
import ir.global.GlobalSymbol


object NullValue : PrimitiveConstant { //TODO remove it
    override fun type(): PointerType {
        return Type.Ptr
    }

    override fun data(): String = "0"

    override fun toString(): String {
        return "null"
    }

    val NULLPTR = NullValue //TODO remove it
}

class PointerLiteral(val gConstant: GlobalSymbol): PrimitiveConstant {
    override fun data(): String = gConstant.name()
    override fun toString(): String = gConstant.name()
    override fun type(): PointerType = Type.Ptr
}