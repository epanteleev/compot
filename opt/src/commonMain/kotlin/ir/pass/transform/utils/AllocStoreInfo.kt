package ir.pass.transform.utils

import ir.instruction.Alloc
import ir.instruction.Store
import ir.module.FunctionData
import ir.pass.isLocalVariable
import ir.module.block.AnyBlock


internal class AllocStoreInfo private constructor(val blocks: FunctionData) {
    private val stores: Map<Alloc, Set<AnyBlock>> by lazy { allStoresInternal() }

    private inline fun forEachAlloc(closure: (Alloc) -> Unit) {
        for (bb in blocks) {
            for (inst in bb) {
                if (inst !is Alloc) {
                    continue
                }

                if (!inst.isLocalVariable()) {
                    continue
                }

                closure(inst)
            }
        }
    }

    private fun allStoresInternal(): Map<Alloc, Set<AnyBlock>> {
        val allStores = hashMapOf<Alloc, MutableSet<AnyBlock>>()
        forEachAlloc { alloc ->
            val stores = mutableSetOf<AnyBlock>()
            for (user in alloc.usedIn()) {
                if (user !is Store) {
                    continue
                }
                if (!user.isLocalVariable()) {
                    break
                }
                stores.add(user.owner())
            }
            allStores[alloc] = stores
        }

        return allStores
    }

    /** Find all bd where are stores of given local variable. */
    fun allStores(): Map<Alloc, Set<AnyBlock>> {
        return stores
    }

    companion object {
        fun create(blocks: FunctionData): AllocStoreInfo {
            return AllocStoreInfo(blocks)
        }
    }
}