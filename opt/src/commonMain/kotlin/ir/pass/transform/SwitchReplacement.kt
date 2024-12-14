package ir.pass.transform

import common.forEachWith
import ir.instruction.*
import ir.module.BasicBlocks
import ir.module.Module
import ir.pass.common.TransformPassFabric
import ir.pass.common.TransformPass


class SwitchReplacement internal constructor(module: Module) : TransformPass(module) {
    override fun name(): String = "switch-replacement"
    override fun run(): Module {
        for (func in module.functions()) {
            SwitchReplacementImpl(func.blocks).run()
        }

        return module
    }
}

object SwitchReplacementFabric : TransformPassFabric() {
    override fun create(module: Module): TransformPass {
        return SwitchReplacement(module.copy())
    }
}

private class SwitchReplacementImpl(val cfg: BasicBlocks) {
    private fun allSwitches(): List<Switch> {
        val switches = arrayListOf<Switch>()
        for (bb in cfg) {
            for (inst in bb) {
                if (inst is Switch) {
                    switches.add(inst)
                }
            }
        }

        return switches
    }

    private fun replaceSwitch(switch: Switch) {
        val selector = switch.value()
        val default  = switch.default()

        var current = switch.owner()
        switch.table().forEachWith(switch.targets()) { value, target ->
            if (target == default) {
                return@forEachWith
            }

            // TODO: Implement phi node handling
            target.phis { throw IllegalStateException("Switch target has phi nodes: target=$target") }

            val newBB = cfg.createBlock()
            if (switch.owner() == current) {
                val inst = current.putBefore(current.last(), IntCompare.icmp(selector, IntPredicate.Eq, value))

                val br = BranchCond.br(inst, target, newBB)
                current.replace(current.last(), br)

            } else {
                val inst = current.put(IntCompare.icmp(selector, IntPredicate.Eq, value))
                current.put(BranchCond.br(inst, target, newBB))
            }
            current = newBB
        }

        current.put(Branch.br(default))
    }

    fun run() {
        allSwitches().forEach { replaceSwitch(it) }
    }
}