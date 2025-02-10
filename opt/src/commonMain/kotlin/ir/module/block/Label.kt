package ir.module.block

import ir.module.LabelResolver

sealed interface Label {
    val index: Int

    fun resolve(labelResolver: LabelResolver): Block

    companion object {
        val entry = BlockViewer(0)
    }
}

class BlockViewer(override val index: Int): Label {
    override fun hashCode(): Int = index

    override fun toString(): String {
        return if (index == 0) {
            "entry"
        } else {
            "L$index"
        }
    }

    override fun resolve(labelResolver: LabelResolver): Block {
        return labelResolver.findBlock(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        return when (other) {
            is Label -> index == other.index
            else     -> false
        }
    }
}