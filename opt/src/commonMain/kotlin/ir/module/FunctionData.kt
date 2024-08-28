package ir.module

import ir.value.ArgumentValue
import ir.module.auxiliary.CopyCFG
import ir.module.block.Block
import ir.module.block.iterator.*
import ir.pass.AnalysisResult
import ir.pass.FunctionAnalysisPassFabric


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

    inline fun <reified T: AnalysisResult, reified U: FunctionAnalysisPassFabric<T>> analysis(analysisType: U): T {
        // Todo cache analysis results
        return analysisType.create(this).run()
    }

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