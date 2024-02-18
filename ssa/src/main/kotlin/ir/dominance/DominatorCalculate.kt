package ir.dominance

import ir.module.BasicBlocks
import ir.module.block.AnyBlock


interface DominatorCalculate {
    fun initializeDominator(length: Int): MutableMap<Int, Int> {
        val dominators = hashMapOf<Int, Int>()

        for (idx in 0 until length) {
            if (idx == length - 1) { /* this is first block */
                dominators[length - 1] = length - 1
            } else {
                dominators[idx] = UNDEFINED
            }
        }

        return dominators
    }

    fun intersect(postDominators: Map<Int, Int>, _finger1: Int, _finger2: Int): Int {
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

    fun indexBlocks(blocksOrder: List<AnyBlock>): Map<AnyBlock, Int> {
        val blockToIndex = hashMapOf<AnyBlock, Int>()
        for ((idx, bb) in blocksOrder.withIndex()) {
            blockToIndex[bb] = idx
        }

        return blockToIndex
    }

    fun evaluateIdom(dominators: Map<Int, Int>, incomingMap: Map<Int, List<Int>>, idx: Int): Int {
        val incoming = incomingMap[idx]!!

        val definedSuccessors = incoming.filter { dominators[it] != UNDEFINED }
        return definedSuccessors.fold(definedSuccessors.first()) { idom, pred ->
            intersect(dominators, pred, idom)
        }
    }

    fun enumerationToIdomMap(blocks: List<AnyBlock>, indexToBlock: Map<Int, AnyBlock>, dominators: MutableMap<Int, Int>): Map<AnyBlock, AnyBlock> {
        dominators.remove(blocks.size - 1)

        val domTree = hashMapOf<AnyBlock, AnyBlock>()
        for (entry in dominators) {
            domTree[indexToBlock[entry.key]!!] = indexToBlock[entry.value]!!
        }

        return domTree
    }

    fun calculateIncomings(postorder: List<AnyBlock>, blockToIndex: Map<AnyBlock, Int>): Map<Int, List<Int>>

    fun blockOrdering(basicBlocks: BasicBlocks): List<AnyBlock>

    fun calculate(basicBlocks: BasicBlocks): Map<AnyBlock, AnyBlock> {
        val blocksOrder = blockOrdering(basicBlocks)
        val blockToIndex = indexBlocks(blocksOrder)

        val length = blocksOrder.size
        val predecessorsMap = calculateIncomings(blocksOrder, blockToIndex)
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

        val indexToBlock = blockToIndex.map { (key, value) -> value to key }.toMap(hashMapOf())
        return enumerationToIdomMap(blocksOrder, indexToBlock, dominators)
    }

    companion object {
        const val UNDEFINED = Int.MAX_VALUE
    }
}