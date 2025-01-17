package ir.pass.analysis.traverse

import ir.module.MutationMarker
import ir.module.block.Block
import ir.pass.common.AnalysisResult


class BlockOrder(private val order: List<Block>, marker: MutationMarker): AnalysisResult(marker), Collection<Block> {
    override fun toString(): String = buildString {
        for (bb in order) {
            append("BB: $bb\n")
        }
    }

    override fun iterator(): Iterator<Block> {
        return order.iterator()
    }

    override val size: Int
        get() = order.size

    operator fun get(index: Int): Block {
        return order[index]
    }

    override fun isEmpty(): Boolean {
        return order.isEmpty()
    }

    override fun containsAll(elements: Collection<Block>): Boolean {
        return order.containsAll(elements)
    }

    override fun contains(element: Block): Boolean {
        return order.contains(element)
    }
}