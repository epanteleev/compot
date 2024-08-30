package asm.x64

sealed class AnyDirective {
    abstract override fun hashCode(): Int
    abstract override fun equals(other: Any?): Boolean
}

sealed class NamedDirective: AnyDirective() {
    abstract val name: String

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Directive

        return name == other.name
    }

    final override fun hashCode(): Int {
        return name.hashCode()
    }
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
    }
}

class Directive(override val name: String, val data: List<String>, val type: List<SymbolType>): NamedDirective() {
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

class CommSymbol(val name: String, val size: Int): AnyDirective() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as CommSymbol

        return name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String {
        return ".comm $name, $size, 32"
    }
}

class AsciiSymbol(override val name: String, val data: String): NamedDirective() {
    override fun toString(): String {
        return "$name:\n\t.ascii \"$data\""
    }
}