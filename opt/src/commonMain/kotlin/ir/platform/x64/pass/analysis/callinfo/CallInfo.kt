package ir.platform.x64.pass.analysis.callinfo

import asm.x64.GPRegister
import asm.x64.XmmRegister
import ir.Definitions.QWORD_SIZE
import ir.instruction.Callable
import ir.module.MutationMarker
import ir.pass.common.AnalysisResult
import ir.platform.x64.CallConvention


class SavedContext internal constructor(val callerSaveGPRegisters: Set<GPRegister>, val callerSavedXmmRegisters: Set<XmmRegister>, private val frameSize: Int, private val overFlowAreaSize: Int) {
    fun adjustStackSize(): Int {
        var sizeToAdjust = callerSavedXmmRegisters.size * QWORD_SIZE + overFlowAreaSize
        val remains = (frameSize + overFlowAreaSize) % CallConvention.STACK_ALIGNMENT
        if (remains != 0L) {
            sizeToAdjust += remains.toInt()
        }

        return sizeToAdjust
    }

    override fun toString(): String = buildString {
        append("SavedContext:\n")
        append("  Caller Save GP Registers: $callerSaveGPRegisters\n")
        append("  Caller Saved XMM Registers: $callerSavedXmmRegisters\n")
        append("  Frame Size: $frameSize\n")
        append("  Overflow Area Size: $overFlowAreaSize\n")
        append("  Adjust Stack Size: ${adjustStackSize()}\n")
    }
}

class CallInfo internal constructor(private val savedContexts: Map<Callable, SavedContext>, marker: MutationMarker): AnalysisResult(marker) {
    fun context(call: Callable): SavedContext = savedContexts[call]!!

    override fun toString(): String = buildString {
        append("CallInfo:\n")
        for ((call, context) in savedContexts) {
            append("  Call: $call\n")
            append(context.toString())
        }
    }
}