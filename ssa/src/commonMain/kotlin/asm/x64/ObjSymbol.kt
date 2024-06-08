package asm.x64

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

data class ObjSymbol(val name: String, val data: String, val type: SymbolType) {
    override fun toString(): String {
        return "$name:\n\t$type $data"
    }
}