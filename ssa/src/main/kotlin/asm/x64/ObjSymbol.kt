package asm.x64

enum class SymbolType {
    String {
        override fun toString(): kotlin.String {
            return ".string"
        }
    },
    Long {
        override fun toString(): kotlin.String {
            return ".quad"
        }
    },
    Integer {
        override fun toString(): kotlin.String {
            return ".long"
        }
    },
    Short {
        override fun toString(): kotlin.String {
            return ".short"
        }
    },
    Byte {
        override fun toString(): kotlin.String {
            return ".byte"
        }
    }
}

data class ObjSymbol(val name: String, val data: String, val type: SymbolType) {
    override fun toString(): String {
        return "$name:\n\t$type $data"
    }
}