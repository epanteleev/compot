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
    private val oldCallableToNew = hashMapOf<IdentityCallable, Callable>()

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
        when (inst) {
            is Phi -> {
                val targets = inst.incoming()
                val newTargets = targets.mapTo(arrayListOf()) { oldToNewBlock[it]!! }
                val newInst = inst.copy(inst.operands(), newTargets) /** put in old usages **/

                oldValuesToNew[inst] = newInst
                return newInst
            }
            is TerminateInstruction -> {
                val newUsages = newUsages(inst)
                val targets = inst.targets()
                val newTargets = targets.map { oldToNewBlock[it]!! }.toTypedArray()

                return inst.copy(newUsages, newTargets)
            }
            is ValueInstruction -> {
                val newUsages = newUsages(inst)
                val newInst = inst.copy(newUsages)
                oldValuesToNew[inst] = newInst as ValueInstruction

                if (newInst is Callable) {
                    oldCallableToNew[IdentityCallable(inst as Callable)] = newInst
                }
                return newInst
            }
            is Callable -> {
                val newUsages = newUsages(inst)
                val newInst = inst.copy(newUsages)
                oldCallableToNew[IdentityCallable(inst as Callable)] = newInst as Callable
                return newInst
            }
            is AdjustStackFrame -> {
                val newUsages = oldCallableToNew[IdentityCallable(inst.call())]!!
                return inst.copy(newUsages)
            }
            else -> {
                val newUsages = newUsages(inst)
                val newInst = inst.copy(newUsages)
                return newInst
            }
        }
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