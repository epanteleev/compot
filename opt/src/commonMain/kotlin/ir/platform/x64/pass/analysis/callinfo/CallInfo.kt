package ir.platform.x64.pass.analysis.callinfo

import asm.x64.GPRegister
import asm.x64.XmmRegister
import ir.Definitions.QWORD_SIZE
import ir.instruction.Callable
import ir.module.MutationMarker
import ir.pass.common.AnalysisResult
import ir.platform.x64.CallConvention


class SavedContext internal constructor(val savedGPRegisters: Set<GPRegister>, val savedXmmRegisters: Set<XmmRegister>, private val frameSize: Int) {
    fun adjustStackSize(overflowAreaSize: Int): Int {
        var sizeToAdjust = savedXmmRegisters.size * QWORD_SIZE + overflowAreaSize
        val remains = (frameSize + overflowAreaSize) % CallConvention.STACK_ALIGNMENT
        if (remains != 0L) {
            sizeToAdjust += remains.toInt()
        }

        return sizeToAdjust
    }
}

class CallInfo internal constructor(private val overFlowAreaSize: Map<Callable, Int>, private val savedContexts: Map<Callable, SavedContext>, marker: MutationMarker): AnalysisResult(marker) {
    fun overflowAreaSize(call: Callable): Int = overFlowAreaSize[call]!!

    fun context(call: Callable): SavedContext = savedContexts[call]!!

    override fun toString(): String {
        TODO("Not yet implemented")
    }
}