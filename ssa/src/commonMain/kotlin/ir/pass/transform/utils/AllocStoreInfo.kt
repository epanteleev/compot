package ir.pass.transform.utils

import ir.instruction.Alloc
import ir.instruction.Store
import ir.module.BasicBlocks
import ir.pass.isLocalVariable
import ir.module.block.AnyBlock


class AllocStoreInfo private constructor(val blocks: BasicBlocks) {
    private val allocated: List<Alloc> by lazy { allocatedVariablesInternal() }
    private val stores: Map<Alloc, Set<AnyBlock>> by lazy { allStoresInternal(allocated) }

    private fun allocatedVariablesInternal(): List<Alloc> {
        val stores = arrayListOf<Alloc>()
        fun allocatedInGivenBlock(bb: AnyBlock) {
            for (inst in bb) {
                if (inst !is Alloc) {
                    continue
                }

                if (!inst.isLocalVariable()) {
                    continue
                }

                stores.add(inst)
            }
        }

        for (bb in blocks) {
            allocatedInGivenBlock(bb)
        }

        return stores
    }

    private fun allStoresInternal(variables: List<Alloc>): Map<Alloc, Set<AnyBlock>> {
        val stores = hashMapOf<Alloc, MutableSet<AnyBlock>>()
        for (v in variables) {
            stores[v] = mutableSetOf()
            for (user in v.usedIn()) {
                if (user !is Store) {
                    continue
                }
                if (!user.isLocalVariable()) {
                    continue
                }
                stores[v]!!.add(user.owner())
            }
        }

        return stores
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