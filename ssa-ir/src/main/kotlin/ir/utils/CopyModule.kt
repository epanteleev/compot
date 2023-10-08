package ir.utils

import ir.*
import ir.block.Block
import ir.instruction.*

internal object CopyModule {
    fun copy(module: Module): Module {
        return Module(module.functions.map { copy(it) }, module.externFunctions.map { it }.toSet())
    }

    fun copy(old: FunctionData): FunctionData {
        return FunctionData.create(old.prototype, copy(old.blocks), old.arguments())
    }

    fun copy(oldBasicBlocks: BasicBlocks): BasicBlocks {
        val oldToNew       = setupNewBasicBlock(oldBasicBlocks)
        val oldValuesToOld = hashMapOf<LocalValue, LocalValue>()

        val arrayBlocks = arrayListOf<Block>()
        for (bb in oldBasicBlocks.preorder()) {
            arrayBlocks.add(oldToNew[bb]!!)
            copy(bb, oldValuesToOld, oldToNew)
        }

        return BasicBlocks.create(arrayBlocks)
    }

    private fun copy(thisBlock: Block, oldToNewValues: MutableMap<LocalValue, LocalValue>, oldToNewBlock: Map<Block, Block>) {
        val newBB = oldToNewBlock[thisBlock]!!
        for (inst in thisBlock.instructions()) {
            newBB.add(newInst(oldToNewValues, oldToNewBlock, inst))
        }
    }

    private fun newUsages(oldToNewValues: MutableMap<LocalValue, LocalValue>, usages: List<Value>): List<Value> {
        return usages.mapTo(arrayListOf()) {
            if (it is ArgumentValue || it is Constant) {
                it
            } else {
                oldToNewValues[it]!!
            }
        }
    }

    private fun newInst(oldToNewValues: MutableMap<LocalValue, LocalValue>, oldToNewBlock: Map<Block, Block>, inst: Instruction): Instruction {
        val newUsages = newUsages(oldToNewValues, inst.usages())
        when (inst) {
            is Phi -> {
                val targets = inst.incoming()
                val newTargets = targets.mapTo(arrayListOf()) { oldToNewBlock[it]!! }

                val newInst = inst.copy(newUsages, newTargets)
                oldToNewValues[inst] = newInst
                return newInst
            }
            is TerminateInstruction -> {
                val targets = inst.targets()
                val newTargets = targets.map { oldToNewBlock[it]!! }.toTypedArray()

                return inst.copy(newUsages, newTargets)
            }
            else -> {
                val newInst = inst.copy(newUsages)
                if (inst is ValueInstruction) {
                    oldToNewValues[inst] = newInst as ValueInstruction
                }

                return newInst
            }
        }
    }

    private fun setupNewBasicBlock(oldBasicBlocks: BasicBlocks): Map<Block, Block> {
        val newBasicBlocks = oldBasicBlocks.blocks().mapTo(arrayListOf()) {
            Block.empty(it.index)
        }

        val oldToNew = hashMapOf<Block, Block>()
        for ((old, new) in oldBasicBlocks.blocks() zip newBasicBlocks) {
            oldToNew[old] = new
        }

        return oldToNew
    }
}