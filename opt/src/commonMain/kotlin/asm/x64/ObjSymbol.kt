package asm.x64

sealed class AnyObjSymbol {
    abstract fun name(): String
}

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
    },
    Asciiz {
        override fun toString(): String {
            return ".asciiz"
        }
    }
}

class ObjSymbol(val name: String, val data: List<String>, val type: List<SymbolType>): AnyObjSymbol() {

    override fun name(): String {
        return name
    }

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("$name:\n")
        var i = 0
        for (d in data) {
            val t = type[i]
            stringBuilder.append("\t$t $d\n")
            i++
        }
        for (i in i until type.size) {
            stringBuilder.append("\t${type[i]} 0\n")
        }
        return stringBuilder.toString()
    }
}

class CommSymbol(val name: String, val size: Int): AnyObjSymbol() {
    override fun name(): String {
        return name
    }

    override fun toString(): String {
        return ".comm $name, $size, 32"
    }
}