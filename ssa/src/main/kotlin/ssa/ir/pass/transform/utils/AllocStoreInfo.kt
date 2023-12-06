package ir.pass.transform.utils

import ir.*
import ir.instruction.*
import ir.module.BasicBlocks
import ir.module.block.AnyBlock

class AllocStoreInfo private constructor(val blocks: BasicBlocks) {
    private val allocated: Set<Alloc> by lazy { allocatedVariablesInternal() }
    private val stores: Map<Alloc, Set<AnyBlock>> by lazy { allStoresInternal(allocated) }

    private fun allocatedVariablesInternal(): Set<Alloc> {
        val stores = hashSetOf<Alloc>()
        fun allocatedInGivenBlock(bb: AnyBlock) {
            for (inst in bb.valueInstructions()) {
                if (!Utils.isStackAllocOfLocalVariable(inst)) {
                    continue
                }

                stores.add(inst as Alloc)
            }
        }

        for (bb in blocks) {
            allocatedInGivenBlock(bb)
        }

        return stores
    }

    private fun allStoresInternal(variables: Set<Alloc>): Map<Alloc, Set<AnyBlock>> {
        val stores = hashMapOf<Alloc, MutableSet<AnyBlock>>()
        for (v in variables) {
            stores[v] = mutableSetOf()
        }

        for (bb in blocks.preorder()) {
            for (inst in bb) {
                if (Utils.isStoreOfLocalVariable(inst)) {
                    inst as Store
                    if (!variables.contains(inst.pointer())) {
                        continue
                    }

                    stores[inst.pointer()]!!.add(bb)
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
    fun allStores(): Map<Alloc, Set<AnyBlock>> {
        return stores
    }

    companion object {
        fun create(blocks: BasicBlocks): AllocStoreInfo {
            return AllocStoreInfo(blocks)
        }
    }
}