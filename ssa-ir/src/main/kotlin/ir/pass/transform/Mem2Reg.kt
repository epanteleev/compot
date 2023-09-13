package ir.pass.transform

import ir.*
import ir.utils.*
import kotlin.math.max

data class Mem2RegException(override val message: String): Exception(message)

private object Utils {
    fun isStackAllocOfLocalVariable(instruction: Instruction): Boolean {
        return instruction is StackAlloc && instruction.size == 1L
    }

    fun isLoadOfLocalVariable(instruction: Instruction): Boolean {
        return instruction is Load && instruction.operand() !is ArgumentValue
    }

    fun isStoreOfLocalVariable(instruction: Instruction): Boolean {
        return instruction is Store && instruction.pointer() !is ArgumentValue
    }
}

private class RewriteAssistant(cfg: BasicBlocks, private val dominatorTree: DominatorTree) {
    private val bbToMapValues = initialize(cfg)

    init {
        for (bb in cfg.preorder()) {
            rewriteValuesSetup(bb)
        }
    }

    private fun rewriteValuesSetup(bb: BasicBlock) {
        for (instruction in bb) {
            if (Utils.isStoreOfLocalVariable(instruction)) {
                instruction as Store
                val actual = findActualValueOrNull(bb, instruction.value())
                if (actual != null) {
                    addValues(bb, instruction.pointer(), actual)
                } else {
                    addValues(bb, instruction.pointer(), instruction.value())
                }

                continue
            }

            if (Utils.isStackAllocOfLocalVariable(instruction)) {
                instruction as StackAlloc
                addValues(bb, instruction, Value.UNDEF)
                continue
            }

            if (Utils.isLoadOfLocalVariable(instruction)) {
                instruction as Load
                val actual = findActualValue(bb, instruction.operand())
                addValues(bb, instruction, actual)
                continue
            }

            if (instruction is Phi) {
                // Note: all used values are equal in uncompleted phi instruction.
                // Will take only first value.
                addValues(bb, instruction.usedValues().first(), instruction)
                continue
            }

            instruction.rewriteUsages { v -> rename(bb, v) }
        }
    }

    private fun initialize(cfg: BasicBlocks): MutableMap<BasicBlock, MutableMap<Value, Value>> {
        val bbToMapValues = hashMapOf<BasicBlock, MutableMap<Value, Value>>()
        for (bb in cfg) {
            bbToMapValues[bb] = hashMapOf()
        }

        return bbToMapValues
    }

    fun findActualValue(bb: Label, value: Value): Value {
        return findActualValueOrNull(bb, value)
            ?: throw Mem2RegException("cannot find: basicBlock=$bb, value=$value")
    }

    fun findActualValueOrNull(bb: Label, value: Value): Value? {
        for (d in dominatorTree.dominators(bb)) {
            val newV = bbToMapValues[d]!![value]
            if (newV != null) {
                return newV
            }
        }

        return null
    }

    fun rename(bb: BasicBlock, oldValue: Value): Value {
        return if (oldValue is ValueInstruction) {
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
            for (instruction in bb.valueInstructions()) {
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

    private fun completePhis(bbToMapValues: RewriteAssistant, bb: BasicBlock) {
        for (instruction in bb) {
            if (instruction !is Phi) {
                break
            }

            instruction.rewriteUsagesInPhi { v, l -> bbToMapValues.rename(l as BasicBlock, v) }
        }
    }

    private fun removeMemoryInstructions(bb: BasicBlock) {
        fun filter(instruction: Instruction): Boolean {
            return when {
                Utils.isStackAllocOfLocalVariable(instruction) -> true
                Utils.isStoreOfLocalVariable(instruction) -> true
                Utils.isLoadOfLocalVariable(instruction)  -> true
                else -> false
            }
        }

        bb.removeIf { filter(it) }
    }

    private fun removeRedundantPhis(defUseInfo: DefUseInfo, deadPool: MutableSet<Instruction>, bb: BasicBlock) {
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

        val bbToMapValues = RewriteAssistant(cfg, dominatorTree)

        for (bb in cfg) {
            completePhis(bbToMapValues, bb)
        }

        for (bb in cfg) {
            removeMemoryInstructions(bb)
        }

        val defUseInfo = cfg.defUseInfo()
        val deadPool = hashSetOf<Instruction>()
        for (bb in cfg.postorder()) {
            removeRedundantPhis(defUseInfo, deadPool, bb)
        }
    }

    companion object {
        fun run(module: Module): Module {
            module.functions.forEach { fnData ->
                val cfg = fnData.blocks

                val dominatorTree = cfg.dominatorTree()
                val joinSet = JoinPointSet.evaluate(cfg, dominatorTree)
                Mem2Reg(cfg, joinSet).pass(dominatorTree)
            }

            return module
        }
    }
}