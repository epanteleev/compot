package types

interface TypeProperty

enum class PointerQualifier: TypeProperty {
    CONST {
        override fun toString() = "const"
    },
    VOLATILE {
        override fun toString() = "volatile"
    },
    RESTRICT {
        override fun toString() = "restrict"
    },
    EMPTY {
        override fun toString(): String = ""
    }
}

enum class StorageClass: TypeProperty {
    TYPEDEF {
        override fun toString(): String = "typedef"
    },
    EXTERN {
        override fun toString(): String = "extern"
    },
    STATIC {
        override fun toString(): String = "static"
    },
    REGISTER {
        override fun toString(): String = "register"
    },
    AUTO {
        override fun toString(): String = "auto"
    }
}