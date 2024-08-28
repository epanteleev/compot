package ir.pass.transform

import common.assertion
import ir.module.*
import ir.instruction.Phi
import ir.module.block.Block
import ir.pass.analysis.traverse.PreOrderFabric
import ir.pass.analysis.traverse.iterator.PreorderIterator
import ir.value.Value


class PhiFunctionPruning private constructor(private val cfg: FunctionData) {
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
        for (bb in cfg.analysis(PreOrderFabric)) {
            for (inst in bb) {
                if (inst is Phi) {
                    usefull.markUseless(inst, bb)
                    continue
                }

                inst.operands { op ->
                    if (op !is Phi) {
                        return@operands
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

            phi.operands { op ->
                if (op !is Phi) {
                    return@operands
                }
                val definedIn = phiPlacesInfo[op] ?: throw RuntimeException("cannon find op=${op}")

                if (usefull.isUseful(op, definedIn)) {
                    return@operands
                }
                usefull.markUseful(op, definedIn)
                worklist.add(op)
            }
        }
    }

    private fun prunePhis() {
        fun removePhi(phi: Phi, bb: Block) {
            assertion(phi.usedIn().fold(true) { acc, value -> acc && (value is Phi) }) {
                "phi value is used in non phi instruction"
            }

            bb.kill(phi, Value.UNDEF)
        }

        usefull.forEachUseless { phi, bb -> removePhi(phi, bb) }
    }

    private fun run() {
        initialSetup()
        usefulnessPropagation()
        prunePhis()
    }

    companion object {
        fun run(module: Module): Module {
            for (f in module.functions()) {
                PhiFunctionPruning(f).run()
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