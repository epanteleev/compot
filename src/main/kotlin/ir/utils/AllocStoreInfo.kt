package ir.utils

import ir.*

class AllocStoreInfo private constructor(val blocks: BasicBlocks) {
    private var allocated: Set<Value>? = null
    private var stores: Map<Value, Set<BasicBlock>>? = null

    private fun allocatedVariablesInternal(): Set<Value> {
        val stores = hashSetOf<Value>()
        fun allocatedInGivenBlock(bb: BasicBlock) {
            for (inst in bb) {
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

    private fun allStoresInternal(variables: Set<Value>): Map<Value, Set<BasicBlock>> {
        val stores = hashMapOf<Value, MutableSet<BasicBlock>>()
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
        if (allocated == null) {
            allocated = allocatedVariablesInternal()
        }

        return allocated as Set<Value>
    }

    /** Find all bd where are stores of given local variable. */
    fun allStores(): Map<Value, Set<BasicBlock>> {
        if (stores == null) {
            stores = allStoresInternal(allocatedVariables())
        }

        return stores as Map<Value, Set<BasicBlock>>
    }

    companion object {
        fun create(blocks: BasicBlocks): AllocStoreInfo {
            return AllocStoreInfo(blocks)
        }
    }
}