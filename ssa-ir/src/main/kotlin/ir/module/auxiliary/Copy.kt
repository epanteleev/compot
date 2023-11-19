package ir.module.auxiliary

import ir.*
import ir.module.block.Block
import ir.instruction.*
import ir.module.BasicBlocks
import ir.module.FunctionData

internal object Copy {
    fun copy(old: FunctionData): FunctionData {
        return FunctionData.create(old.prototype, copy(old.blocks), old.arguments())
    }

    fun copy(oldBasicBlocks: BasicBlocks): BasicBlocks {
        val oldToNew       = setupNewBasicBlock(oldBasicBlocks)
        val oldValuesToNew = hashMapOf<LocalValue, LocalValue>()

        val arrayBlocks = arrayListOf<Block>()
        for (bb in oldBasicBlocks.preorder()) {
            arrayBlocks.add(oldToNew[bb]!!)
            copy(bb, oldValuesToNew, oldToNew)
        }

        val newBB = BasicBlocks.create(arrayBlocks)
        updatePhis(newBB, oldValuesToNew)

        return newBB
    }

    private fun copy(thisBlock: Block, oldToNewValues: MutableMap<LocalValue, LocalValue>, oldToNewBlock: Map<Block, Block>) {
        val newBB = oldToNewBlock[thisBlock]!!
        for (inst in thisBlock.instructions()) {
            newBB.add(newInst(oldToNewValues, oldToNewBlock, inst))
        }
    }

    private fun newUsages(oldToNewValues: Map<LocalValue, LocalValue>, inst: Instruction): List<Value> {
        return inst.operands().mapTo(arrayListOf()) {
            if (it is ArgumentValue || it is Constant) {
                it
            } else {
                val result = oldToNewValues[it]
                assert(result != null) {
                    "cannot find value=$it in instruction=$inst"
                }

                result as Value
            }
        }
    }

    private fun newInst(oldToNewValues: MutableMap<LocalValue, LocalValue>, oldToNewBlock: Map<Block, Block>, inst: Instruction): Instruction {
        when (inst) {
            is Phi -> {
                val targets = inst.incoming()
                val newTargets = targets.mapTo(arrayListOf()) { oldToNewBlock[it]!! }
                val newInst = inst.copy(inst.operands(), newTargets) /** put in old usages **/

                oldToNewValues[inst] = newInst
                return newInst
            }
            is TerminateInstruction -> {
                val newUsages = newUsages(oldToNewValues, inst)
                val targets = inst.targets()
                val newTargets = targets.map { oldToNewBlock[it]!! }.toTypedArray()

                return inst.copy(newUsages, newTargets)
            }
            else -> {
                val newUsages = newUsages(oldToNewValues, inst)
                val newInst = inst.copy(newUsages)
                if (inst is ValueInstruction) {
                    oldToNewValues[inst] = newInst as ValueInstruction
                }

                return newInst
            }
        }
    }

    private fun updatePhis(arrayBlocks: BasicBlocks, oldValuesToOld: Map<LocalValue, LocalValue>) {
        for (bb in arrayBlocks) {
            for (inst in bb.instructions()) {
                if (inst !is Phi) {
                    continue
                }

                val usages = newUsages(oldValuesToOld, inst)
                inst.update(usages, inst.incoming())
            }
        }
    }

    private fun setupNewBasicBlock(oldBasicBlocks: BasicBlocks): Map<Block, Block> {
        val newBasicBlocks = arrayListOf<Block>()
        val oldToNew = hashMapOf<Block, Block>()

        for (old in oldBasicBlocks.blocks()) {
            val new = Block.empty(old.index, old.maxValueIndex())
            newBasicBlocks.add(new)
            oldToNew[old] = new
        }

        return oldToNew
    }
}