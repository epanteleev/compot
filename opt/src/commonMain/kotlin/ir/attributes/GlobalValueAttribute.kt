package ir.attributes

enum class GlobalValueAttribute : AnyAttribute {
    EXTERNAL {
        override fun toString(): String = "external"
    },
    INTERNAL {
        override fun toString(): String = "internal"
    },
}