package types

interface TypeProperty


// 6.7.3 Type qualifiers
// https://port70.net/~nsz/c/c11/n1570.html#6.7.3
enum class TypeQualifier: TypeProperty {
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

// Storage class specifiers
// https://port70.net/~nsz/c/c11/n1570.html#6.7.1
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

// 6.7.4 Function specifiers
// https://port70.net/~nsz/c/c11/n1570.html#6.7.4
enum class FunctionSpecifier: TypeProperty {
    INLINE {
        override fun toString(): String = "inline"
    },
    NORETURN {
        override fun toString(): String = "_Noreturn"
    }
}