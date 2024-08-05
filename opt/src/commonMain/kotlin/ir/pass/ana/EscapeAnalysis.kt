package ir.pass.ana

import ir.value.*
import ir.instruction.*
import ir.module.FunctionData
import ir.types.PrimitiveType


enum class EscapeState {
    Global,   // Global means the value escapes the function
    Local,    // Local means the value is local to the function
    Field,    // Field means the value is a field of a local value
    Argument, // Argument means the value is passed as an argument
    Constant, // Constant means the value is a constant
    Unknown;  // Unknown means the value is unknown

    fun union(other: EscapeState): EscapeState {
        if (this == other) {
            return this
        }
        return when {
            this == Local || other == Global -> Global
            this == Global || other == Local -> Global
            this == Argument || other == Local -> Argument
            this == Local || other == Argument -> Argument
            this == Field || other == Local -> Field
            this == Local || other == Field -> Field
            else -> throw IllegalStateException("Cannot union $this and $other")
        }
    }
}

// Escape analysis pass
// A simple escape analysis pass that determines whether a value escapes the function
// The pass is based on the following rules:
// - If a value is allocated in the function, it is local
// - If a value is stored in the function, it is local
// - If a value is loaded in the function, it is local if the pointer is local
// - If a value is passed as an argument, it is an argument
// - If a value is a constant, it is a constant
// - Otherwise, the value is unknown
class EscapeAnalysis private constructor(private val functionData: FunctionData) {
    private val escapeState = hashMapOf<Value, EscapeState>()

    private fun union(operand: Value, newState: EscapeState): EscapeState {
        val state = escapeState[operand] ?: EscapeState.Unknown
        return state.union(newState)
    }

    private fun run(): Map<Value, EscapeState> {
        for (block in functionData.blocks.preorder()) {
            for (instruction in block) {
                when (instruction) {
                    is Alloc -> {
                        if (instruction.isLocalVariable()) {
                            escapeState[instruction] = EscapeState.Local
                        } else {
                            escapeState[instruction] = EscapeState.Global
                        }
                    }

                    is Store -> {
                        if (instruction.isLocalVariable()) {
                            escapeState[instruction.pointer()] = EscapeState.Local
                        } else {
                            escapeState[instruction.pointer()] = EscapeState.Global
                        }
                    }

                    is Load -> {
                        if (instruction.isLocalVariable()) {
                            escapeState[instruction] = EscapeState.Local
                        } else {
                            escapeState[instruction] = EscapeState.Global
                        }
                    }
                }
            }
        }
        return escapeState
    }

    companion object {
        fun run(module: FunctionData): EscapeAnalysisResult {
            val escapeState = EscapeAnalysis(module).run()
            return EscapeAnalysisResult(escapeState)
        }
    }
}

class EscapeAnalysisResult(private val escapeState: Map<Value, EscapeState>) {
    fun getEscapeState(value: Value): EscapeState {
        if (value is Constant) {
            return EscapeState.Constant
        }
        return escapeState[value] ?: EscapeState.Unknown
    }
}


fun Alloc.canBeReplaced(): Boolean {
    return allocatedType is PrimitiveType
}

fun Alloc.isLocalVariable(): Boolean {
    return canBeReplaced() && noEscape()
}

fun Load.isLocalVariable(): Boolean {
    return canBeReplaced() && (operand() as Alloc).isLocalVariable()
}

fun Load.canBeReplaced(): Boolean {
    val operand = operand()
    if (operand is Generate) {
        return true
    }

    return operand is Alloc
}

fun Store.isLocalVariable(): Boolean {
    val pointer = pointer()
    return (pointer is Alloc && pointer.isLocalVariable()) || pointer is Generate
}

/** Check whether alloc result isn't leak to other function. **/
fun Alloc.noEscape(): Boolean {
    for (user in usedIn()) {
        if (user is Load) {
            continue
        }

        if (user is Store && user.pointer() == this) {
            continue
        }
        return false
    }
    return true
}