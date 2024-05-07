package ir.pass.transform.auxiliary

import ir.ArgumentValue
import ir.instruction.*
import ir.module.FunctionData
import ir.module.Module
import ir.module.SSAModule
import ir.module.block.Block
import ir.module.block.Label


internal class FunctionsIsolation private constructor(private val cfg: FunctionData) {
    private fun isolateArgumentValues() {
        if (isLeafCall()) {
            // Not necessary to insert copies
            return
        }

        val begin = cfg.blocks.begin()
        val mapArguments = hashMapOf<ArgumentValue, ValueInstruction>()

        for (arg in cfg.arguments()) { //Todo O(argSize * countOfInstructions) ?!?
            mapArguments[arg] = begin.insert(0) { it.copy(arg) }
        }

        for (bb in cfg.blocks) { //TODO we don't need to iterate over all instructions if 'ArgumentValue' will hold their users
            for (inst in bb.instructions()) {
                if (bb.equals(Label.entry) && inst is Copy) {
                    continue
                }

                inst.update { value ->
                    if (value !is ArgumentValue) {
                        return@update value
                    }

                    mapArguments[value]!!
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
    }

    companion object {
        fun run(module: Module): Module {
            module.functions.forEach { FunctionsIsolation(it).pass() }
            return SSAModule(module.functions, module.externFunctions, module.globals, module.types)
        }
    }
}