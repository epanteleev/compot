package ir.pass.transform.auxiliary

import ir.Value
import ir.instruction.*
import ir.ArgumentValue
import ir.module.Module
import ir.module.SSAModule
import ir.module.block.Block
import ir.module.block.Label
import ir.module.FunctionData


internal class CopyInsertion private constructor(private val cfg: FunctionData) {
    private fun isolatePhis(bb: Block, phi: Phi) {
        val newValues = hashMapOf<Value, Value>()
        phi.forAllIncoming { incoming, operand ->
            assert(!bb.hasCriticalEdgeFrom(incoming)) {
                "Flow graph has critical edge from $incoming to $bb"
            }

            val copy = incoming.insert(incoming.instructions().size - 1) {
                it.copy(operand)
            }

            newValues[operand] = copy
        }

        phi.update { newValues[it]!! }
    }

    private fun isolateArgumentValues() {
        if (isLeafCall()) {
            // Not necessary to insert copies
            return
        }

        val begin = cfg.blocks.begin()
        val mapArguments = hashMapOf<ArgumentValue, ValueInstruction>()
        val copies = mutableSetOf<Copy>()
        for (arg in cfg.arguments()) { //Todo O(argSize * countOfInstructions) ?!?
            val copy = begin.insert(0) {
                it.copy(arg)
            }
            mapArguments[arg] = copy
            copies.add(copy)
        }

        for (bb in cfg.blocks) {
            for (inst in bb.instructions()) {
                if (bb.equals(Label.entry) && copies.contains(inst)) {
                    continue
                }

                for ((idx, use) in inst.operands().withIndex()) { //TODO
                    if (use !is ArgumentValue) {
                        continue
                    }

                    inst.update(idx, mapArguments[use]!!)
                }
            }
        }
    }

    private fun isolateCall() {
        fun insertCopies(bb: Block, idx: Int, call: Callable): Int {
            call as Instruction
            bb.insert(idx) {
                it.downStackFrame(call)
            }

            for ((i, arg) in call.arguments().withIndex()) {
                val copy = bb.insert(idx + 1 + i) {
                    it.copy(arg)
                }
                call.update(i, copy)
            }

            val upStackFramePosition = idx + call.arguments().size + 2
            bb.insert(upStackFramePosition) {
                it.upStackFrame(call)
            }

            return call.arguments().size + 2
        }

        for (bb in cfg.blocks) {
            val instructions = bb.instructions()
            var idx = 0
            while (idx < instructions.size) {
                val c = instructions[idx]
                idx += 1
                if (c !is Callable) {
                    continue
                }

                idx += insertCopies(bb, idx - 1, c)
            }
        }
    }

    private fun isLeafCall(): Boolean {
        for (bb in cfg.blocks) {
            for (inst in bb.instructions()) {
                if (inst is Callable) {
                    return false
                }
            }
        }

        return true
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
            return SSAModule(module.functions, module.externFunctions, module.globals, module.types)
        }
    }
}