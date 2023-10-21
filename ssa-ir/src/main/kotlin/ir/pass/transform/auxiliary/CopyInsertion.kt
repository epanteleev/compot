package ir.pass.transform.auxiliary

import ir.*
import ir.module.block.Block
import ir.module.block.Label
import ir.instruction.*
import ir.module.FunctionData
import ir.module.Module

internal class CopyInsertion private constructor(private val cfg: FunctionData) {
    private fun isolatePhis(bb: Block, phi: Phi) {
        val newValues = hashMapOf<Value, Value>()
        for ((incoming, operand) in phi.zip()) {
            assert(!bb.hasCriticalEdgeFrom(incoming)) {
                "Flow graph has critical edge from $incoming to $bb"
            }

            val copy = incoming.insert(incoming.last()) {
                it.copy(operand)
            }

            newValues[operand] = copy
        }

        val newUsages = phi.usages().mapTo(arrayListOf()) { newValues[it]!! }
        phi.update(newUsages)
    }

    private fun isolateArgumentValues() {
        val begin = cfg.blocks.begin()
        val mapArguments = hashMapOf<ArgumentValue, ValueInstruction>()
        val copies = mutableSetOf<Copy>()
        for (arg in cfg.arguments()) { //Todo O(argSize * countOfInstructions) ?!?
            val copy = begin.insert(0) {
                it.copy(arg)
            }
            copy as Copy
            mapArguments[arg] = copy
            copies.add(copy)
        }

        for (bb in cfg.blocks) {
            for (inst in bb.instructions()) {
                if (bb.equals(Label.entry) && copies.contains(inst)) {
                    continue
                }

                for ((idx, use) in inst.usages().withIndex()) {
                    if (use !is ArgumentValue) {
                        continue
                    }

                    inst.update(idx, mapArguments[use]!!)
                }
            }
        }
    }

    private fun isolateCall() {
        fun insertCopies(bb: Block, call: Callable) {
            call as Instruction
            bb.insert(call) {
                it.downStackFrame(call)
                Value.UNDEF
            }

            for ((idx, arg) in call.arguments().withIndex()) {
                val copy = bb.insert(call) {
                    it.copy(arg)
                }
                call.update(idx, copy)
            }

            bb.insert(bb.indexOf(call) + 1) {
                it.upStackFrame(call)
                Value.UNDEF
            }
        }

        for (bb in cfg.blocks) {
            val calls = bb.instructions().filterIsInstance<Callable>()
            for (c in calls) {
                c as Instruction
                insertCopies(bb, c)
            }
        }
    }

    fun pass() {
        isolateArgumentValues()
        isolateCall()

        for (bb in cfg.blocks) {
            bb.phis { phi ->
                isolatePhis(bb, phi)
            }
        }
    }

    companion object {
        fun run(module: Module): Module {
            module.functions.forEach { CopyInsertion(it).pass() }
            return module
        }
    }
}