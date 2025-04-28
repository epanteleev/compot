package ir.pass.analysis.dominance

import common.intMapOf
import ir.module.FunctionData
import ir.module.block.Block
import ir.module.block.Label
import ir.pass.analysis.traverse.BlockOrder
import ir.pass.common.AnalysisResult
import ir.pass.common.FunctionAnalysisPass


sealed class DominatorCalculate<T: AnalysisResult>: FunctionAnalysisPass<T>() {
    private fun initializeDominator(length: Int): MutableMap<Int, Int> {
        val dominators = intMapOf<Int, Int>(length) { it }

        for (idx in 0 until length) {
            if (idx == length - 1) { /* this is first block */
                dominators[length - 1] = length - 1
            } else {
                dominators[idx] = UNDEFINED
            }
        }

        return dominators
    }

    private fun intersect(postDominators: Map<Int, Int>, _finger1: Int, _finger2: Int): Int {
        var finger1 = _finger1
        var finger2 = _finger2

        while (finger1 != finger2) {
            while (finger1 < finger2) {
                finger1 = postDominators[finger1]!!
            }

            while (finger2 < finger1) {
                finger2 = postDominators[finger2]!!
            }
        }

        return finger1
    }

    private fun indexBlocks(blocksOrder: BlockOrder): Map<Block, Int> {
        val blockToIndex = intMapOf<Block, Int>(blocksOrder.size) { it : Label -> it.index }
        for ((idx, bb) in blocksOrder.withIndex()) {
            blockToIndex[bb] = idx
        }

        return blockToIndex
    }

    private fun evaluateIdom(dominators: Map<Int, Int>, incomingMap: Map<Int, List<Int>>, idx: Int): Int {
        val incoming = incomingMap[idx]!!

        val definedSuccessors = incoming.filter { dominators[it] != UNDEFINED }
        return definedSuccessors.fold(definedSuccessors.first()) { idom, pred ->
            intersect(dominators, pred, idom)
        }
    }

    private fun enumerationToIdomMap(blocks: BlockOrder, indexToBlock: Map<Int, Block>, dominators: MutableMap<Int, Int>): Map<Block, Block> {
        dominators.remove(blocks.size - 1)

        val dominatorTree = intMapOf<Block, Block>(blocks.size) { l: Label -> l.index }
        for (entry in dominators) {
            dominatorTree[indexToBlock[entry.key]!!] = indexToBlock[entry.value]!!
        }

        return dominatorTree
    }

    abstract fun calculateIncoming(postorder: BlockOrder, blockToIndex: Map<Block, Int>): Map<Int, List<Int>> //TODO not necessary to evaluate it

    abstract fun blockOrdering(basicBlocks: FunctionData): BlockOrder

    private fun evalIndexToBlock(blockToIndex: Map<Block, Int>): Map<Int, Block> {
        val indexToBlock = intMapOf<Int, Block>(blockToIndex.size) { it }
        for ((key, value) in blockToIndex) {
            indexToBlock[value] = key
        }

        return indexToBlock
    }

    fun calculate(basicBlocks: FunctionData): Map<Block, Block> {
        val blocksOrder = blockOrdering(basicBlocks)
        val blockToIndex = indexBlocks(blocksOrder)

        val length = blocksOrder.size
        val predecessorsMap = calculateIncoming(blocksOrder, blockToIndex)
        val dominators = initializeDominator(length)
        var changed = true
        while (changed) {
            changed = false
            for (idx in (0 until length - 1).reversed()) {
                val newDom = evaluateIdom(dominators, predecessorsMap, idx)

                if (newDom != dominators[idx]) {
                    dominators[idx] = newDom
                    changed = true
                }
            }
        }

        val indexToBlock = evalIndexToBlock(blockToIndex)
        return enumerationToIdomMap(blocksOrder, indexToBlock, dominators)
    }

    companion object {
        const val UNDEFINED = Int.MAX_VALUE
    }
}