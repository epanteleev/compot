package ir.utils

import ir.*
import ir.block.AnyBlock

class AllocStoreInfo private constructor(val blocks: BasicBlocks) {
    private val allocated: Set<ValueInstruction> by lazy { allocatedVariablesInternal() }
    private val stores: Map<ValueInstruction, Set<AnyBlock>> by lazy { allStoresInternal(allocated) }

    private fun allocatedVariablesInternal(): Set<ValueInstruction> {
        val stores = hashSetOf<ValueInstruction>()
        fun allocatedInGivenBlock(bb: AnyBlock) {
            for (inst in bb.valueInstructions()) {
                if (inst is StackAlloc && inst.size == 1L) {
                    stores.add(inst)
                }
            }
        }

        for (bb in blocks) {
            allocatedInGivenBlock(bb)
        }

        return stores
    }

    private fun allStoresInternal(variables: Set<ValueInstruction>): Map<ValueInstruction, Set<AnyBlock>> {
        val stores = hashMapOf<ValueInstruction, MutableSet<AnyBlock>>()
        for (v in variables) {
            stores[v] = mutableSetOf()
        }

        for (bb in blocks.preorder()) {
            for (inst in bb) {
                if (inst is Store && inst.pointer() !is ArgumentValue) {
                    if (variables.contains(inst.pointer())) {
                        stores[inst.pointer()]!!.add(bb)
                    }
                }
            }
        }

        return stores
    }

    /** Returns set of variables which produced by 'stackalloc' instruction. */
    fun allocatedVariables(): Set<Value> {
        return allocated
    }

    /** Find all bd where are stores of given local variable. */
    fun allStores(): Map<ValueInstruction, Set<AnyBlock>> {
        return stores
    }

    companion object {
        fun create(blocks: BasicBlocks): AllocStoreInfo {
            return AllocStoreInfo(blocks)
        }
    }
}