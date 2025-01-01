package ir.attributes

enum class GlobalValueAttribute : AnyAttribute, FunctionAttribute {
    INTERNAL {
        override fun toString(): String = "!internal"
    },
    DEFAULT {
        override fun toString(): String = "!default"
    },
    HIDDEN {
        override fun toString(): String = "!hidden"
    },
    PROTECTED {
        override fun toString(): String = "!protected"
    },
    WEAK {
        override fun toString(): String = "!weak"
    },
}