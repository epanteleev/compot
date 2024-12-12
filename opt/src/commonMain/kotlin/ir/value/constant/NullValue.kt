package ir.value.constant

import ir.types.*
import ir.global.GlobalSymbol


object NullValue : PrimitiveConstant { //TODO remove it
    override fun type(): PtrType = PtrType

    override fun toString(): String = "null"
}

class PointerLiteral private constructor(val gConstant: GlobalSymbol, val index: Int = 0): PrimitiveConstant {
    override fun toString(): String = if (index == 0) {
        gConstant.name()
    } else {
        "gep ${gConstant.name()}, $index"
    }

    override fun type(): PtrType = PtrType

    companion object {
        fun of(gConstant: GlobalSymbol): PointerLiteral {
            return PointerLiteral(gConstant)
        }

        fun of(gConstant: GlobalSymbol, index: Int): PointerLiteral {
            return PointerLiteral(gConstant, index)
        }
    }
}