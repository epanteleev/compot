package ir.platform.x64.pass.analysis.callinfo

import asm.x64.*
import common.assertion
import ir.Definitions
import ir.Definitions.POINTER_SIZE
import ir.Definitions.QWORD_SIZE
import ir.instruction.Callable
import ir.module.FunctionData
import ir.module.Sensitivity
import ir.pass.analysis.LivenessAnalysisPassFabric
import ir.pass.common.AnalysisType
import ir.pass.common.FunctionAnalysisPass
import ir.pass.common.FunctionAnalysisPassFabric
import ir.platform.x64.CallConvention.gpCallerSaveRegs
import ir.platform.x64.CallConvention.xmmCallerSaveRegs
import ir.platform.x64.codegen.CodegenException
import ir.platform.x64.pass.analysis.regalloc.LinearScanFabric
import ir.types.AggregateType
import ir.types.UndefType
import ir.value.LocalValue


private class CallInfoAnalysisImpl(private val data: FunctionData): FunctionAnalysisPass<CallInfo>() {
    private val registerAllocation = data.analysis(LinearScanFabric)
    private val liveness = data.analysis(LivenessAnalysisPassFabric)

    private val savedContexts = hashMapOf<Callable, SavedContext>()

    private fun overflowAreaSize(call: Callable): Int {
        val regs = getArgRegisters(call)

        var argumentsSlotsSize = 0
        for ((idx, reg) in regs.withIndex()) {
            if (reg !is Address) {
                continue
            }
            val prototype = call.prototype()
            val byVal = prototype.byValue(idx)
            if (byVal == null) {
                argumentsSlotsSize += POINTER_SIZE
                continue
            }

            val type = prototype.argument(idx) ?: throw CodegenException("argument type is null")
            assertion(type is AggregateType) { "type=$type" }

            argumentsSlotsSize += Definitions.alignTo(type.sizeOf(), QWORD_SIZE)
        }

        return argumentsSlotsSize
    }


    private fun frameSize(savedRegisters: Set<GPRegister>, savedXmmRegisters: Set<XmmRegister>): Int {
        return (savedRegisters.size + savedXmmRegisters.size + registerAllocation.calleeSaveRegs().size + /** include retaddr and rbp **/ 2) * QWORD_SIZE +
                registerAllocation.spilledLocalsSize()
    }

    private fun getArgRegisters(call: Callable): List<VReg?> {
        val regs = arrayListOf<VReg?>()
        for (arg in call.arguments()) {
            if (arg !is LocalValue) {
                regs.add(null)
            } else {
                regs.add(registerAllocation.vRegOrNull(arg))
            }
        }

        return regs
    }

    private fun callerSaveRegisters(liveOutOperands: Collection<LocalValue>, call: Callable): SavedContext {
        val registers = hashSetOf<GPRegister>()
        val xmmRegisters = hashSetOf<XmmRegister>()

        val exclude = call as? LocalValue
        for (value in liveOutOperands) {
            if (value.type() == UndefType) {
                continue
            }

            if (exclude == value) {
                continue
            }

            when (val reg = registerAllocation.vRegOrNull(value)!!) {
                is GPRegister -> {
                    if (!gpCallerSaveRegs.contains(reg)) {
                        continue
                    }

                    registers.add(reg)
                }
                is XmmRegister -> {
                    if (!xmmCallerSaveRegs.contains(reg)) {
                        continue
                    }

                    xmmRegisters.add(reg)
                }
                else -> {}
            }
        }

        val frameSize = frameSize(registers, xmmRegisters)
        return SavedContext(registers, xmmRegisters, frameSize, overflowAreaSize(call))
    }

    override fun run(): CallInfo {
        for (bb in data) {
            for (inst in bb) {
                if (inst !is Callable) {
                    continue
                }

                val liveOut = liveness.liveOut(inst.owner())
                savedContexts[inst] = callerSaveRegisters(liveOut, inst)
            }
        }

        return CallInfo(savedContexts, data.marker())
    }
}

object CallInfoAnalysis: FunctionAnalysisPassFabric<CallInfo>() {
    override fun type(): AnalysisType {
        return AnalysisType.CALL_INFO
    }

    override fun sensitivity(): Sensitivity {
        return Sensitivity.CONTROL_AND_DATA_FLOW
    }

    override fun create(functionData: FunctionData): CallInfo {
        return CallInfoAnalysisImpl(functionData).run()
    }
}