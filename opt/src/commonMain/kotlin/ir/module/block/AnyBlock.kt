package ir.module.block

import common.LListNode
import common.LeakedLinkedList

abstract class AnyBlock<Inst : LListNode>(override val index: Int): Label, Iterable<Inst> {
    protected val instructions = object : LeakedLinkedList<Inst>() {}

    abstract fun predecessors(): List<AnyBlock<Inst>>
    abstract fun successors(): List<AnyBlock<Inst>>

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other is BlockViewer) {
            return index == other.index
        }

        if (other == null || this::class != other::class) return false
        other as Block

        return index == other.index
    }

    final override fun hashCode(): Int {
        return index
    }

    final override operator fun iterator(): Iterator<Inst> {
        return instructions.iterator()
    }

    val size
        get(): Int = instructions.size
}