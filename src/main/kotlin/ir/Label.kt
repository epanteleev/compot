package ir

abstract class Label(open val index: Int) {
    abstract fun index(): Int

    override fun toString(): String {
        val idx = index()
        return if (idx == 0) {
            "entry"
        } else {
            "L$idx"
        }
    }

    companion object {
        val entry = BlockViewer(0)
    }
}

class BlockViewer(index: Int): Label(index) {
    override fun index(): Int {
        return index
    }

    override fun hashCode(): Int {
        return index
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        val flag = when (other) {
            is BlockViewer -> index == other.index
            is BasicBlock  -> index == other.index()
            else           -> false
        }
        return flag
    }
}