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
        //TODO we don't have to isolate arguments if it isn't live-out from 'call' instruction

        val begin = cfg.begin()
        for (arg in cfg.arguments()) {
            begin.updateUsages(arg) { begin.prepend { it.copy(arg) } }
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
                bb.updateDF(call, i, copy)
            }
            call.target().prepend { it.upStackFrame(call) }
            return call
        }

        for (bb in cfg) {
            bb.transform { inst -> insertCopies(bb, inst) }
        }
    }

    private fun isLeafCall(): Boolean {
        for (bb in cfg) {
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
            return SSAModule(module.functions, module.externFunctions, module.constantPool, module.globals, module.types)
        }
    }
}