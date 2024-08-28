package asm.x64

import common.assertion
import common.forEachWith

enum class SymbolType {
    StringLiteral {
        override fun toString(): String {
            return ".string"
        }
    },
    Quad {
        override fun toString(): String {
            return ".quad"
        }
    },
    Long {
        override fun toString(): String {
            return ".long"
        }
    },
    Short {
        override fun toString(): String {
            return ".short"
        }
    },
    Byte {
        override fun toString(): String {
            return ".byte"
        }
    }
}

data class ObjSymbol(val name: String, val data: List<String>, val type: List<SymbolType>) {
    init {
        assertion(data.size == type.size) { "invariant" }
    }

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("$name:\n")
        data.forEachWith(type) { d, t ->
            stringBuilder.append("\t$t $d\n")
        }
        return stringBuilder.toString()
    }
}