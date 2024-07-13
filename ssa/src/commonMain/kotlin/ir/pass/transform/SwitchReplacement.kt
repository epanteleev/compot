package ir.pass.transform

import common.forEachWith
import ir.instruction.Switch
import ir.module.BasicBlocks
import ir.module.Module
import ir.pass.PassFabric
import ir.pass.TransformPass


class SwitchReplacement(module: Module) : TransformPass(module) {
    override fun name(): String = "switch-replacement"
    override fun run(): Module {
        TODO()
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

        switch.table().forEachWith(switch.targets()) { value, target ->
            val newBB = cfg.createBlock()
            //val cmp = newBB.icmp(selector, value)
        }
    }

    fun run() {
        val switches = allSwitches()
        for (switch in switches) {
            replaceSwitch(switch)
        }
    }
}

