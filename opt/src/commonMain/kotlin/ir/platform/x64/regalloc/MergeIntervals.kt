package ir.platform.x64.regalloc

import common.assertion
import ir.instruction.*
import ir.module.FunctionData
import ir.value.TupleValue
import ir.value.LocalValue
import ir.pass.analysis.intervals.MergedLiveIntervals


class MergeIntervals private constructor(private val fd: FunctionData) {
    private val groups = hashMapOf<LocalValue, Group>()

    private fun build(): MergedLiveIntervals {
        coalescingInstructionIntervals()
        return MergedLiveIntervals(groups)
    }

    private fun handlePhiOperands(phi: Phi) {
        val groupList = arrayListOf<LocalValue>(phi)
        phi.operands { used ->
            if (used !is LocalValue) {
                return@operands
            }
            assertion(used is Copy) { "expect this invariant: used=$used" }
            groupList.add(used)
        }

        val group = Group(groupList)
        phi.operands { used ->
            if (used !is LocalValue) {
                return@operands
            }
            groups[used] = group
        }
        groups[phi] = group
    }

    private fun handleTuple(value: TupleValue) {
        value.proj { proj ->
            groups[proj] = Group(arrayListOf<LocalValue>(proj))
        }
    }

    private fun coalescingInstructionIntervals() {
        for (bb in fd) {
            for (value in bb) {
                when (value) {
                    is Phi        -> handlePhiOperands(value)
                    is TupleValue -> handleTuple(value)
                }
            }
        }
    }

    companion object {
        fun evaluate(fd: FunctionData): MergedLiveIntervals {
            return MergeIntervals(fd).build()
        }
    }
}