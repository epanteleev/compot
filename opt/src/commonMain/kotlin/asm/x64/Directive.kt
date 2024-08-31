package asm.x64

sealed class AnyDirective

sealed class SectionDirective: AnyDirective()

sealed class NamedDirective(): AnyDirective() {
    val anonymousDirective = arrayListOf<AnonymousDirective>()

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

sealed class AnonymousDirective: AnyDirective()

data object TextSection : SectionDirective() {
    override fun toString(): String = ".text"
}

data object DataSection : SectionDirective() {
    override fun toString(): String = ".data"
}

data object BssSection : SectionDirective() {
    override fun toString(): String = ".bss"
}

class GlobalDirective(val name: String) : AnonymousDirective() {
    override fun toString(): String = ".global $name"
}

data object ExternDirective : AnonymousDirective() {
    override fun toString(): String = ".extern"
}

class ObjLabel(override val name: String): NamedDirective() {
    override fun toString(): String {
        return buildString {
            append("$name:\n")
            for ((idx, d) in anonymousDirective.withIndex()) {
                append("$d")
                if (idx != anonymousDirective.size - 1) {
                    append("\n")
                }
            }
        }
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

class StringSymbol(override val name: String, val data: String): NamedDirective() {
    override fun toString(): String = "$name:\n\t.string $data"
}

class QuadSymbol(override val name: String, val data: String): NamedDirective() {
    override fun toString(): String = "$name:\n\t.quad $data"
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

class CommSymbol(override val name: String, val size: Int): NamedDirective() {
    override fun toString(): String {
        return ".comm $name, $size, 32"
    }
}

class AsciiSymbol(override val name: String, val data: String): NamedDirective() {
    override fun toString(): String {
        return "$name:\n\t.ascii \"$data\""
    }
}