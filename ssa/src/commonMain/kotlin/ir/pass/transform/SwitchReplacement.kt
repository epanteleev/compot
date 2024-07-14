package ir.pass.transform

import common.forEachWith
import ir.instruction.IntPredicate
import ir.instruction.Switch
import ir.module.BasicBlocks
import ir.module.Module
import ir.pass.PassFabric
import ir.pass.TransformPass


class SwitchReplacement(module: Module) : TransformPass(module) {
    override fun name(): String = "switch-replacement"
    override fun run(): Module {
        for (func in module.functions) {
            SwitchReplacementImpl(func.blocks).run()
        }

        return module
    }
}

object SwitchReplacementFabric : PassFabric {
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
        val default = switch.default()
        val targets = switch.targets().mapTo(arrayListOf()) { it }
        val table = switch.table().mapTo(arrayListOf()) { it }

        var current = switch.owner()
        table.forEachWith(targets) { value, target ->
            if (target == default) {
                return@forEachWith
            }

            val newBB = cfg.createBlock()
            if (switch.owner() == current) {
                val inst = current.insertBefore(current.last()) { it.icmp(selector, IntPredicate.Eq, value) }
                current.update(current.last()) { it.branchCond(inst, target, newBB) }
            } else {
                val inst = current.icmp(selector, IntPredicate.Eq, value)
                current.branchCond(inst, target, newBB)
            }
            current = newBB
        }

        current.branch(default)
    }

    fun run() {
        val switches = allSwitches()
        for (switch in switches) {
            replaceSwitch(switch)
        }
    }
}

