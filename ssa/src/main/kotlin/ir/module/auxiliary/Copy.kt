package ir.module.auxiliary

import ir.*
import ir.instruction.*
import ir.module.BasicBlocks
import ir.module.FunctionData
import ir.module.block.Block

internal object Copy {
    fun copy(old: FunctionData): FunctionData {
        return FunctionData.create(old.prototype, copy(old.blocks), old.arguments())
    }

    fun copy(oldBasicBlocks: BasicBlocks): BasicBlocks {
        return CopyCFG(oldBasicBlocks).copy()
    }
}

private class CopyCFG(val oldBasicBlocks: BasicBlocks) {
    private val oldValuesToNew = hashMapOf<LocalValue, LocalValue>()
    private val oldToNewBlock = setupNewBasicBlock()

    private fun setupNewBasicBlock(): Map<Block, Block> {
        val oldToNew = hashMapOf<Block, Block>()
        for (old in oldBasicBlocks.blocks()) {
            oldToNew[old] = Block.empty(old.index, old.maxValueIndex())
        }

        return oldToNew
    }

    fun copy(): BasicBlocks {
        val arrayBlocks = arrayListOf<Block>()
        for (bb in oldBasicBlocks.preorder()) {
            arrayBlocks.add(oldToNewBlock[bb]!!)
            copy(bb)
        }

        val newBB = BasicBlocks.create(arrayBlocks)
        updatePhis(newBB)

        return newBB
    }

    private fun copy(thisBlock: Block) {
        val newBB = oldToNewBlock[thisBlock]!!
        for (inst in thisBlock.instructions()) {
            newBB.add(newInst(inst))
        }
    }

    private fun newUsages(inst: Instruction): List<Value> {
        return inst.operands().mapTo(arrayListOf()) {
            if (it is ArgumentValue || it is Constant || it is GlobalSymbol) {
                it
            } else {
                val result = oldValuesToNew[it]
                assert(result != null) {
                    "cannot find value=$it in instruction=$inst"
                }

                result as Value
            }
        }
    }

    private fun newInst(inst: Instruction): Instruction {
        return InstructionCopy.copy(oldValuesToNew, oldToNewBlock, inst)
    }

    private fun updatePhis(arrayBlocks: BasicBlocks) {
        for (bb in arrayBlocks) {
            for (inst in bb.instructions()) {
                if (inst !is Phi) {
                    continue
                }

                val usages = newUsages(inst)
                inst.update(usages, inst.incoming())
            }
        }
    }
}