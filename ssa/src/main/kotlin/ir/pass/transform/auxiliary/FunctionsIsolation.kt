package ir.pass.transform.auxiliary

import ir.instruction.*
import ir.module.FunctionData
import ir.module.Module
import ir.module.SSAModule
import ir.module.block.Block


internal class FunctionsIsolation private constructor(private val cfg: FunctionData) {
    private fun isolateArgumentValues() {
        if (isLeafCall()) {
            // Not necessary to insert copies
            return
        }

        val begin = cfg.blocks.begin()
        for (arg in cfg.arguments()) {
            val copy = begin.prepend { it.copy(arg) }
            arg.replaceUsages(copy)
        }
    }

    private fun isolateCall() {
        fun insertCopies(bb: Block, call: Instruction): Instruction {
            if (call !is Callable) {
                return call
            }
            bb.insertBefore(call) { it.downStackFrame(call) }

            for ((i, arg) in call.arguments().withIndex()) {
                val copy = bb.insertBefore(call) { it.copy(arg) }
                call.update(i, copy)
            }
            call.target().prepend { it.upStackFrame(call) }
            return call
        }

        for (bb in cfg.blocks) {
            bb.transform { inst -> insertCopies(bb, inst) }
        }
    }

    private fun isLeafCall(): Boolean {
        for (bb in cfg.blocks) {
            if (bb.last() is Callable) {
                return false
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