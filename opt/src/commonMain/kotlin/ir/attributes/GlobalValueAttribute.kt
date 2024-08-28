package ir.attributes

enum class GlobalValueAttribute : AnyAttribute {
    INTERNAL {
        override fun toString(): String = "internal"
    },
}