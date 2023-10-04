package ir

import java.util.HashMap

class DominatorTree(private val idomMap: MutableMap<BasicBlock, BasicBlock>) {
    private val cachedDominators = hashMapOf<Label, List<Label>>()

    private fun calculateDominators(target: Label): List<Label> {
        val dom = arrayListOf<Label>()
        var current: Label? = target
        while (current != null) {
            dom.add(current)
            current = idomMap[current]
        }

        return dom
    }

    fun dominates(dominator: Label, target: Label): Boolean {
        var current: Label? = target

        while (current != null) {
            if (current == dominator) {
                return true
            }

            current = idomMap[current]
        }

        return false
    }

    fun dominators(target: Label): List<Label> {
        val saved = cachedDominators[target]
        if (saved != null) {
            return saved
        }

        val dom = calculateDominators(target)
        cachedDominators[target] = dom
        return dom
    }

    fun frontiers(): Map<BasicBlock, List<BasicBlock>> {
        val dominanceFrontiers = hashMapOf<BasicBlock, MutableList<BasicBlock>>()

        idomMap.forEach { (bb, idom) ->
            val predecessors = bb.predecessors()
            if (predecessors.size < 2) {
                return@forEach
            }

            for (p in predecessors) {
                var runner: BasicBlock = p
                while (runner != idom) {
                    (dominanceFrontiers.getOrPut(runner) { arrayListOf() }).add(bb)
                    runner = idomMap[runner]!!
                }
            }
        }

        return dominanceFrontiers
    }

    operator fun iterator(): MutableIterator<MutableMap.MutableEntry<BasicBlock, BasicBlock>> {
        return idomMap.iterator()
    }

    companion object {
        private const val UNDEFINED = Int.MAX_VALUE

        fun evaluate(basicBlocks: BasicBlocks): DominatorTree {
            fun initializeDominator(length: Int): MutableMap<Int, Int> {
                val dominators = hashMapOf<Int, Int>()

                for (idx in 0 until length) {
                    if (idx == length - 1) /* this is first block */
                        dominators[length - 1] = length - 1
                    else
                        dominators[idx] = UNDEFINED
                }

                return dominators
            }

            fun calculateSuccessors(postorder: List<BasicBlock>, blockToIndex: Map<BasicBlock, Int>): Map<Int, List<Int>> {
                val predecessors = hashMapOf<Int, List<Int>>()

                for (bb in postorder) {
                    if (bb.predecessors().isNotEmpty()) {
                        predecessors[blockToIndex[bb]!!] = bb.predecessors().map { blockToIndex[it]!! }
                    }
                }

                return predecessors
            }

            fun intersect(dominators: Map<Int, Int>, _finger1: Int, _finger2: Int): Int {
                var finger1 = _finger1
                var finger2 = _finger2

                while (finger1 != finger2) {
                    while (finger1 < finger2) {
                        finger1 = dominators[finger1]!!
                    }

                    while (finger2 < finger1) {
                        finger2 = dominators[finger2]!!
                    }
                }

                return finger1
            }

            fun evaluateIdom(dominators: Map<Int, Int>, successorsMap: Map<Int, List<Int>>, idx: Int): Int {
                val predecessors = successorsMap[idx]!!

                val definedSuccessors = predecessors.filter { dominators[it] != UNDEFINED }

                val lambda: (Int, Int) -> Int = {
                    idom, pred -> intersect(dominators, pred, idom)
                }

                return definedSuccessors.fold(definedSuccessors.first(), lambda)
            }

            fun enumerationToBlocks(blocks: List<BasicBlock>, indexToBlock: Map<Int, BasicBlock>, dominators: MutableMap<Int, Int>): DominatorTree {
                dominators.remove(blocks.size - 1)

                val domTree = hashMapOf<BasicBlock, BasicBlock>()
                for (entry in dominators) {
                    domTree[indexToBlock[entry.key]!!] = indexToBlock[entry.value]!!
                }

                return DominatorTree(domTree)
            }

            fun postorder(): List<BasicBlock> {
                val blocksOrder = arrayListOf<BasicBlock>()
                for (bb in basicBlocks.postorder()) {
                    blocksOrder.add(bb)
                }

                return blocksOrder
            }

            fun indexBlocks(blocksOrder: List<BasicBlock>): Map<BasicBlock, Int> {
                val blockToIndex = hashMapOf<BasicBlock, Int>()
                for ((idx, bb) in blocksOrder.withIndex()) {
                    blockToIndex[bb] = idx
                }

                return blockToIndex
            }

            val blocksOrder = postorder()
            val blockToIndex = indexBlocks(blocksOrder)


            val length = blocksOrder.size

            val predecessorsMap = calculateSuccessors(blocksOrder, blockToIndex)
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

            val indexToBlock = blockToIndex.map { (key, value) -> value to key }.toMap(HashMap())

            return enumerationToBlocks(blocksOrder, indexToBlock, dominators)
        }
    }
}