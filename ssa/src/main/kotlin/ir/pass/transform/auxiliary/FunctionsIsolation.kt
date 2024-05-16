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

        for (arg in cfg.arguments()) {
            mapArguments[arg] = begin.prepend { it.copy(arg) }
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
        fun insertCopies(bb: Block, call: Callable): Int {
            call as Instruction
            bb.insertBefore(call) { it.downStackFrame(call) }

            for ((i, arg) in call.arguments().withIndex()) {
                val copy = bb.insertBefore(call) { it.copy(arg) }
                call.update(i, copy)
            }
            bb.insertAfter(call) { it.upStackFrame(call) }

            return call.arguments().size + 2
        }

        for (bb in cfg.blocks) {
            bb.forEachInstruction { inst ->
                val call = inst as? Callable ?: return@forEachInstruction 0
                return@forEachInstruction insertCopies(bb, call)
            }
        }
    }

    private fun isLeafCall(): Boolean {
        for (bb in cfg.blocks) {
            for (inst in bb) {
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