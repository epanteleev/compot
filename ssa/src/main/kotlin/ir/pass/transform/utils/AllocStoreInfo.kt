package ir.pass.transform.utils

import ir.instruction.Alloc
import ir.instruction.Store
import ir.module.BasicBlocks
import ir.module.block.AnyBlock
import ir.pass.ValueInstructionExtension.isLocalVariable


class AllocStoreInfo private constructor(val blocks: BasicBlocks) {
    private val allocated: Set<Alloc> by lazy { allocatedVariablesInternal() }
    private val stores: Map<Alloc, Set<AnyBlock>> by lazy { allStoresInternal(allocated) }

    private fun allocatedVariablesInternal(): Set<Alloc> {
        val stores = hashSetOf<Alloc>()
        fun allocatedInGivenBlock(bb: AnyBlock) {
            for (inst in bb.valueInstructions()) {
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

    private fun allStoresInternal(variables: Set<Alloc>): Map<Alloc, Set<AnyBlock>> {
        val stores = hashMapOf<Alloc, MutableSet<AnyBlock>>()
        for (v in variables) {
            stores[v] = mutableSetOf()
        }

        for (bb in blocks.preorder()) { //TODO fast traversal???
            for (inst in bb) {
                if (inst !is Store) {
                    continue
                }
                if (!inst.isLocalVariable()) {
                    continue
                }
                if (!variables.contains(inst.pointer())) {
                    continue
                }

                stores[inst.pointer()]!!.add(bb)
            }
        }

        return stores
    }

    /** Returns set of variables which produced by 'stackalloc' instruction. */
    fun allocatedVariables(): Set<Alloc> {
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