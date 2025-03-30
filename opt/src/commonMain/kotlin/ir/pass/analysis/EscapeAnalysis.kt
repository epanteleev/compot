package ir.pass.analysis

import ir.value.*
import ir.instruction.*
import ir.module.FunctionData
import ir.module.MutationMarker
import ir.module.Sensitivity
import ir.pass.analysis.traverse.PreOrderFabric
import ir.pass.common.AnalysisResult
import ir.pass.common.AnalysisType
import ir.pass.common.FunctionAnalysisPass
import ir.pass.common.FunctionAnalysisPassFabric
import ir.value.constant.Constant


enum class EscapeState {
    NoEscape,
    Field,
    Argument,
    Constant,
    Unknown;

    internal fun union(other: EscapeState): EscapeState {
        if (this == Unknown || other == Unknown) {
            return Unknown
        }
        if (this == other) {
            return this
        }
        if (this == NoEscape) {
            return other
        }
        if (other == NoEscape) {
            return this
        }

        return when {
            this == Argument || other == Field -> Argument
            this == Field || other == Argument -> Field
            else -> throw IllegalStateException("Cannot union $this and $other")
        }
    }
}

private class EscapeAnalysis(private val functionData: FunctionData): FunctionAnalysisPass<EscapeAnalysisResult>() {
    private val preorder = functionData.analysis(PreOrderFabric)
    private val escapeState = hashMapOf<Value, EscapeState>()

    private fun union(operand: Value, newState: EscapeState): EscapeState {
        val state = escapeState[operand] ?: EscapeState.Unknown
        return state.union(newState)
    }

    private fun visitAlloc(alloc: Alloc) {
        escapeState[alloc] = EscapeState.NoEscape
    }

    private fun visitStore(store: Store) {
        escapeState[store.pointer()] = union(store.pointer(), EscapeState.NoEscape)
        when (val value = store.value()) {
            is Constant -> escapeState[value] = EscapeState.Constant
            is LocalValue -> escapeState[value] = union(value, EscapeState.Field)
        }
    }

    private fun visitLoad(load: Load) {
        val operand = load.operand()
        if (operand is LocalValue) {
            escapeState[operand] = union(operand, EscapeState.NoEscape)
        } else {
            escapeState[operand] = union(operand, EscapeState.Unknown)
        }
    }

    private fun visitPointer2Int(pointer2Int: Pointer2Int) {
        escapeState[pointer2Int.operand()] = union(pointer2Int.operand(), EscapeState.Unknown)
    }

    private fun visitCall(call: Callable) {
        for (argument in call.arguments()) {
            escapeState[argument] = union(argument, EscapeState.Argument)
        }
    }

    private fun visitGetElementPtr(getElementPtr: GetElementPtr) {
        escapeState[getElementPtr.source()] = union(getElementPtr.source(), EscapeState.Field)
    }

    private fun visitGetFieldPtr(getFieldPtr: GetFieldPtr) {
        escapeState[getFieldPtr.source()] = union(getFieldPtr.source(), EscapeState.Field)
    }

    override fun run(): EscapeAnalysisResult {
        for (block in preorder) {
            for (instruction in block) {
                when (instruction) {
                    is Alloc -> visitAlloc(instruction)
                    is Store -> visitStore(instruction)
                    is Load  -> visitLoad(instruction)
                    is Callable -> visitCall(instruction)
                    is Pointer2Int -> visitPointer2Int(instruction)
                    is GetElementPtr -> visitGetElementPtr(instruction)
                    is GetFieldPtr -> visitGetFieldPtr(instruction)
                }
            }
        }
        return EscapeAnalysisResult(escapeState, functionData.marker())
    }
}

class EscapeAnalysisResult(private val escapeState: Map<Value, EscapeState>, marker: MutationMarker): AnalysisResult(marker) {
    override fun toString(): String = buildString {
        for ((value, state) in escapeState) {
            append("Value: $value: $state\n")
        }
    }

    fun getEscapeState(value: Value): EscapeState {
        if (value is Constant) {
            return EscapeState.Constant
        }

        return escapeState[value] ?: EscapeState.Unknown
    }

    fun isNoEscape(value: Value): Boolean {
        return getEscapeState(value) == EscapeState.NoEscape
    }
}

object EscapeAnalysisPassFabric: FunctionAnalysisPassFabric<EscapeAnalysisResult>() {
    override fun type(): AnalysisType {
        return AnalysisType.ESCAPE_ANALYSIS
    }

    override fun sensitivity(): Sensitivity {
        return Sensitivity.CONTROL_AND_DATA_FLOW
    }

    override fun create(functionData: FunctionData): EscapeAnalysisResult {
        return EscapeAnalysis(functionData).run()
    }
}