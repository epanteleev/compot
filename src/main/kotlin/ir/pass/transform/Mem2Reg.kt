package ir.pass.transform

import ir.*
import ir.utils.*
import kotlin.math.max

private class RenameAssistant(cfg: BasicBlocks, private val dominatorTree: DominatorTree) {
    private val bbToMapValues = initialize(cfg)

    private fun initialize(cfg: BasicBlocks): MutableMap<BasicBlock, MutableMap<Value, Value>> {
        val bbToMapValues = hashMapOf<BasicBlock, MutableMap<Value, Value>>()
        for (bb in cfg) {
            bbToMapValues[bb] = hashMapOf()
        }

        return bbToMapValues
    }

    fun findActualValue(bb: Label, value: Value): Value {
        val actual = findActualValueOrNull(bb, value)
        return actual as Value
    }

    private fun findActualValueOrNull(bb: Label, value: Value): Value? {
        for (d in dominatorTree.dominators(bb)) {
            val newV = bbToMapValues[d]!![value]
            if (newV != null) {
                return newV
            }
        }

        return null
    }

    fun rename(bb: BasicBlock, oldValue: Value): Value {
        return if (oldValue is Instruction) {
            findActualValueOrNull(bb, oldValue) ?: oldValue
        } else {
            oldValue
        }
    }

    fun addValues(bb: BasicBlock, from: Value, to: Value) {
        bbToMapValues[bb]!![from] = to
    }
}

class Mem2Reg private constructor(private val cfg: BasicBlocks, private val joinSet: Map<BasicBlock, Set<Value>>) {
    private fun findMaxIndex(): Int {
        var idx = 0
        for (bb in cfg) {
            for (instruction in bb) {
                idx = max(instruction.defined(), idx)
            }
        }

        return idx
    }

    private fun insertPhisIfNeeded(idx: Int, bb: BasicBlock): Int {
        val values = joinSet[bb] ?: return idx

        var newIdx = idx
        for (v in values) {
            val incoming = bb.predecessors.mapTo(arrayListOf()) { v }

            val phi = Phi(newIdx, v.type().dereference(), bb.predecessors, incoming)
            bb.prepend(phi)

            newIdx += 1
        }

        return newIdx
    }

    private fun completePhis(bbToMapValues: RenameAssistant, bb: BasicBlock) {
        for (instruction in bb) {
            if (instruction !is Phi) {
                break
            }

            instruction.renameUsagesInPhi { v, l -> bbToMapValues.rename(l as BasicBlock, v) }
        }
    }

    private fun isStackAllocOfLocalVariable(instruction: Instruction): Boolean {
        return instruction is StackAlloc && instruction.size == 1L
    }

    private fun isLoadOfLocalVariable(instruction: Instruction): Boolean {
        return instruction is Load && instruction.operand() !is ArgumentValue
    }

    private fun isStoreOfLocalVariable(instruction: Instruction): Boolean {
        return instruction is Store && instruction.pointer() !is ArgumentValue
    }

    private fun renameValues(bbToMapValues: RenameAssistant, bb: BasicBlock) {
        for (instruction in bb) {
            if (isStoreOfLocalVariable(instruction)) {
                instruction as Store
                bbToMapValues.addValues(bb, instruction.pointer(), instruction.value())
                continue
            }

            if (isStackAllocOfLocalVariable(instruction)) {
                bbToMapValues.addValues(bb, instruction, Value.UNDEF)
                continue
            }

            if (isLoadOfLocalVariable(instruction)) {
                instruction as Load
                val actual = bbToMapValues.findActualValue(bb, instruction.operand())
                bbToMapValues.addValues(bb, instruction, actual)
                continue
            }

            if (instruction is Phi) {
                bbToMapValues.addValues(bb, instruction.usages.first(), instruction)
                continue
            }

            instruction.renameUsages { v -> bbToMapValues.rename(bb, v) }
        }
    }

    private fun removeMemoryInstructions(bb: BasicBlock) {
        fun filter(instruction: Instruction): Boolean {
            return when {
                isStackAllocOfLocalVariable(instruction) -> true
                isStoreOfLocalVariable(instruction) -> true
                isLoadOfLocalVariable(instruction)  -> true
                else -> false
            }
        }

        bb.removeIf { filter(it) }
    }

    private fun removeRedundantPhis(defUseInfo: DefUseInfo, deadPool: MutableSet<Value>, bb: BasicBlock) {
        fun filter(instruction: Instruction): Boolean {
            if (instruction !is Phi ) {
                return false
            }

            if (defUseInfo.isNotUsed(instruction) || deadPool.containsAll(defUseInfo.usedIn(instruction))) {
                deadPool.add(instruction)
                return true
            }

            return false
        }

        bb.removeIf { filter(it) }
    }

    private fun pass(dominatorTree: DominatorTree) {
        var idx = findMaxIndex() + 1

        for (bb in cfg) {
            idx = insertPhisIfNeeded(idx, bb)
        }

        val bbToMapValues = RenameAssistant(cfg, dominatorTree)

        for (bb in cfg.preorder()) {
            renameValues(bbToMapValues, bb)
        }

        for (bb in cfg) {
            completePhis(bbToMapValues, bb)
        }

        for (bb in cfg) {
            removeMemoryInstructions(bb)
        }

        val defUseInfo = cfg.defUseInfo()
        val deadPool = hashSetOf<Value>()
        for (bb in cfg.postorder()) {
            removeRedundantPhis(defUseInfo, deadPool, bb)
        }
    }

    companion object {
        fun run(module: Module): Module {
            module.functions.forEach { (_, fnData) ->
                val cfg = fnData.blocks

                val dominatorTree = cfg.dominatorTree()
                val joinSet = JoinPointSet.evaluate(cfg, dominatorTree)
                Mem2Reg(cfg, joinSet).pass(dominatorTree)
            }

            return module
        }
    }
}