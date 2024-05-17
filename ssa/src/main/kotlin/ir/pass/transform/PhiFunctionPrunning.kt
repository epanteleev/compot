package ir.pass.transform

import ir.Value
import ir.module.*
import ir.instruction.Phi
import ir.module.block.Block
import ir.instruction.ValueInstruction


class PhiFunctionPruning private constructor(private val cfg: BasicBlocks) {
    private val usefull = UsefulnessMap()
    private val worklist = arrayListOf<Phi>()
    private val phiPlacesInfo = setupPhiPlacesInfo()

    private fun setupPhiPlacesInfo(): Map<Phi, Block> {
        val phiInfo = hashMapOf<Phi, Block>()
        for (bb in cfg) {
            for (inst in bb) {
                if (inst !is Phi) {
                    continue
                }

                phiInfo[inst] = bb
            }
        }

        return phiInfo
    }

    private fun initialSetup() {
        for (bb in cfg.preorder()) {
            for (inst in bb) {
                if (inst is Phi) {
                    usefull.markUseless(inst, bb)
                    continue
                }

                for (op in inst.operands()) {
                    if (op !is Phi) {
                        continue
                    }

                    val place = phiPlacesInfo[op]!!
                    usefull.markUseful(op, place)
                    worklist.add(op)
                }
            }
        }
    }

    private fun usefulnessPropagation() {
        while (worklist.isNotEmpty()) {
            val phi = worklist.last()
            worklist.removeLast()

            for (op in phi.operands()) {
                if (op !is Phi) {
                    continue
                }
                val definedIn = phiPlacesInfo[op] ?: throw RuntimeException("cannon find op=${op}")

                if (usefull.isUseful(op, definedIn)) {
                    continue
                }
                usefull.markUseful(op, definedIn)
                worklist.add(op)
            }
        }
    }

    private fun prunePhis() {
        fun removePhi(phi: Phi, bb: Block) {
            assert(phi.usedIn().fold(true) { acc, value -> acc && (value is Phi) }) {
                "phi value is used in non phi instruction: operands=${phi.operands()}"
            }

            ValueInstruction.replaceUsages(phi, Value.UNDEF)
            bb.kill(phi)
        }

        usefull.forEachUseless { phi, bb -> removePhi(phi, bb)}
    }

    private fun run() {
        initialSetup()
        usefulnessPropagation()
        prunePhis()
    }

    companion object {
        fun run(module: Module): Module {
            for (f in module.functions) {
                PhiFunctionPruning(f.blocks).run()
            }

            return module
        }
    }
}

private class UsefulnessMap {
    private val usefull = hashMapOf<Pair<Phi, Block>, Boolean>()

    fun markUseless(phi: Phi, bb: Block) {
        usefull[Pair(phi, bb)] = false
    }

    fun markUseful(phi: Phi, bb: Block) {
        usefull[Pair(phi, bb)] = true
    }

    fun isUseful(phi: Phi, bb: Block): Boolean {
        return usefull[Pair(phi, bb)]!!
    }

    fun isUseless(phi: Phi, bb: Block): Boolean {
        return !isUseful(phi, bb)
    }

    fun forEachUseless(closure: (Phi, Block) -> Unit) {
        for ((pair, flag) in usefull) {
            if (flag) {
                continue
            }

            closure(pair.first, pair.second)
        }
    }
}