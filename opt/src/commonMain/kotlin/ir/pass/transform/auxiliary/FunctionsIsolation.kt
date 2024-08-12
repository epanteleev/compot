package ir.pass.transform.auxiliary

import ir.instruction.*
import ir.module.FunctionData
import ir.module.Module
import ir.module.SSAModule
import ir.module.block.Block
import ir.pass.analysis.LivenessAnalysisPassFabric
import ir.value.ArgumentValue


internal class FunctionsIsolation private constructor(private val cfg: FunctionData) {
    private val liveness = cfg.analysis(LivenessAnalysisPassFabric)
    private val allCalls = run { //TODO separate analysis pass
        val calls = arrayListOf<Instruction>()
        for (bb in cfg) {
            for (inst in bb) {
                if (inst is Callable) {
                    calls.add(inst)
                }
            }
        }
        calls
    }

    private fun mustBeIsolated(arg: ArgumentValue): Boolean {
        for (call in allCalls) {
            call as Callable
            if (liveness.liveOut(call).contains(arg)) {
                // Argument is live out of the call
                return true
            }
            if (call.arguments().contains(arg)) {
                // Argument is used in the call
                return true
            }
        }

        return false
    }

    private fun isolateArgumentValues() {
        if (allCalls.isEmpty()) {
            // Not necessary to insert copies
            return
        }
        val begin = cfg.begin()
        for (arg in cfg.arguments()) {
            if (!mustBeIsolated(arg)) {
                continue
            }

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