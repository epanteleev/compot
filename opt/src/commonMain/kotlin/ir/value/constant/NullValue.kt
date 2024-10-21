package ir.value.constant

import ir.types.*
import ir.global.GlobalSymbol
import ir.global.GlobalValue


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

class PointerLiteral private constructor(val gConstant: GlobalSymbol, val index: Int = 0): PrimitiveConstant {
    override fun data(): String = toString()
    override fun toString(): String = if (index == 0) {
        gConstant.name()
    } else {
        "gep ${gConstant.name()}, $index"
    }

    override fun type(): PointerType = Type.Ptr

    companion object {
        fun of(gConstant: GlobalSymbol): PointerLiteral {
            return PointerLiteral(gConstant)
        }

        fun of(gConstant: GlobalSymbol, index: Int): PointerLiteral {
            return PointerLiteral(gConstant, index)
        }
    }
}