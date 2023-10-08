package ir.pass.transform

import ir.*
import ir.block.AnyBlock
import ir.block.Block
import ir.block.Label
import ir.instruction.*
import ir.utils.*

data class Mem2RegException(override val message: String): Exception(message)

private object Utils {
    fun isStackAllocOfLocalVariable(instruction: Instruction): Boolean {
        return instruction is StackAlloc && instruction.size() == 1L
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

    private fun rewriteValuesSetup(bb: Block) {
        val instructions = bb.instructions()
        for (index in instructions.indices) {
            val instruction = instructions[index]
            if (instruction is Branch) {
                continue
            }

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
                addValues(bb, instruction.usages().first(), instruction)
                continue
            }

            bb.update(index) {
                instruction.copy(instruction.usages().mapTo(arrayListOf()) { v -> rename(bb, v) } )
            }
        }
    }

    private fun initialize(cfg: BasicBlocks): MutableMap<Block, MutableMap<Value, Value>> {
        val bbToMapValues = hashMapOf<Block, MutableMap<Value, Value>>()
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

    fun rename(bb: Block, oldValue: Value): Value {
        return if (oldValue is ValueInstruction) {
            findActualValueOrNull(bb, oldValue) ?: oldValue
        } else {
            oldValue
        }
    }

    fun addValues(bb: Block, from: Value, to: Value) {
        bbToMapValues[bb]!![from] = to
    }

    override fun toString(): String {
        val builder = StringBuilder()
        for ((bb, valueMap) in bbToMapValues) {
            builder.append("----- bb=$bb -----\n")
            for ((from, to) in valueMap) {
                builder.append("$from -> $to\n")
            }
        }

        return builder.toString()
    }
}

class Mem2Reg private constructor(private val cfg: BasicBlocks, private val joinSet: Map<AnyBlock, Set<Value>>) {
    private fun insertPhisIfNeeded(bb: Block) {
        val values = joinSet[bb] ?: return /** skip phi insertion. **/

        for (v in values) {
            bb.insert(0) {
                it.uncompletedPhi(v, bb)
            }
        }
    }

    private fun completePhis(bbToMapValues: RewriteAssistant, bb: Block) {
        bb.phis { phi ->
            bb.update(phi) {
                it as Phi
                val newUsages = arrayListOf<Value>()
                for ((l, v) in it.zip()) {
                    newUsages.add(bbToMapValues.rename(l, v))
                }
                it.copy(newUsages)
            }
        }

        for (phi in bb.phis()) {
            // Exchange phi functions
            for (used in phi.usages()) {
                if (used !is Phi) {
                    continue
                }
                if (!bb.contains(used)) {
                    continue
                }

                val usedIdx = bb.indexOf(used)
                val phiIndex = bb.indexOf(phi)

                if (usedIdx < phiIndex) {
                    bb.swap(usedIdx, phiIndex)
                }
            }
        }
    }

    private fun removeMemoryInstructions(bb: Block) {
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

    private fun removeRedundantPhis(defUseInfo: DefUseInfo, deadPool: MutableSet<Instruction>, bb: Block) {
        fun filter(instruction: Instruction): Boolean {
            if (instruction !is Phi) {
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
        for (bb in cfg) {
            insertPhisIfNeeded(bb)
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