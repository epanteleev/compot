package ir.pass.transform.auxiliary

import ir.global.GlobalConstant
import ir.module.BasicBlocks
import ir.module.Module


internal class ConstantLoading private constructor(private val cfg: BasicBlocks) {
    private fun pass() {
        for (bb in cfg) {
            var idx = 0
            val instructions = bb.instructions()
            while (idx < instructions.size) {
                val inst = instructions[idx]

                for ((i, use) in inst.operands().withIndex()) {
                    if (use !is GlobalConstant) {
                        continue
                    }

                    val lea = bb.insert(i) { it.lea(use) }
                    inst.update(i, lea)
                    idx++
                }
                idx++
            }
        }
    }

    companion object {
        fun run(module: Module): Module {
            for (fn in module.functions) {
                ConstantLoading(fn.blocks).pass()
            }
            return module
        }
    }
}