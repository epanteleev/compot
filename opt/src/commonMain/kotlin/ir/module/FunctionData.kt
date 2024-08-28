package ir.module

import ir.dominance.DominatorTree
import ir.dominance.PostDominatorTree
import ir.value.ArgumentValue
import ir.module.auxiliary.CopyCFG
import ir.liveness.LiveIntervals
import ir.liveness.LiveIntervalsBuilder
import ir.module.block.Block
import ir.module.block.iterator.*
import ir.pass.ana.LoopDetection
import ir.pass.ana.LoopInfo


class FunctionData private constructor(val prototype: FunctionPrototype, private var argumentValues: List<ArgumentValue>, val blocks: BasicBlocks) {
    fun arguments(): List<ArgumentValue> {
        return argumentValues
    }

    fun copy(): FunctionData {
        return CopyCFG.copy(this)
    }

    fun name(): String {
        return prototype.name
    }

    // Analysis

    fun liveness(): LiveIntervals {
        return LiveIntervalsBuilder.evaluate(this)
    }

    fun preorder(): BasicBlocksIterator {
        return PreorderIterator(begin(), size())
    }

    fun postorder(): BasicBlocksIterator {
        return PostorderIterator(begin(), size())
    }

    fun backwardPostorder(): BasicBlocksIterator {
        return BackwardPostorderIterator(end(), size())
    }

    fun bfsTraversal(): BasicBlocksIterator {
        return BfsTraversalIterator(begin(), size())
    }

    fun linearScanOrder(loopInfo: LoopInfo): BasicBlocksIterator {
        return LinearScanOrderIterator(begin(), size(), loopInfo)
    }

    fun loopInfo(): LoopInfo {
        return LoopDetection.evaluate(this)
    }

    fun dominatorTree(): DominatorTree {
        return DominatorTree.evaluate(this)
    }

    fun postDominatorTree(): PostDominatorTree {
        return PostDominatorTree.evaluate(this)
    }

    //

    operator fun iterator(): Iterator<Block> {
        return blocks.iterator()
    }

    operator fun get(index: Int): Block {
        return blocks.blocks()[index]
    }

    fun size(): Int {
        return blocks.size()
    }

    fun begin(): Block {
        return blocks.begin()
    }

    private fun end(): Block {
        return blocks.end()
    }

    override fun hashCode(): Int {
        return prototype.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as FunctionData
        return prototype == other.prototype
    }

    companion object {
        fun create(prototype: FunctionPrototype, basicBlocks: BasicBlocks, argumentValues: List<ArgumentValue>): FunctionData {
            return FunctionData(prototype, argumentValues, basicBlocks)
        }
    }
}