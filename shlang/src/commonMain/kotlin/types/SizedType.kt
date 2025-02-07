package types

sealed class SizedType : CType() {
    abstract fun size(): Int
    abstract fun alignmentOf(): Int
}