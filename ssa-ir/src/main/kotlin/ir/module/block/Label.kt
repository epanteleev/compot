package ir.module.block

interface Label {
    val index: Int

    companion object {
        val entry = BlockViewer(0)
    }
}

class BlockViewer(override val index: Int): Label {
    override fun hashCode(): Int {
        return index
    }

    override fun toString(): String {
        return if (index == 0) {
            "entry"
        } else {
            "L$index"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        return when (other) {
            is Label -> index == other.index
            else     -> false
        }
    }
}