package types

sealed class CompletedType : CType() {
    abstract fun size(): Int
    abstract fun alignmentOf(): Int
}