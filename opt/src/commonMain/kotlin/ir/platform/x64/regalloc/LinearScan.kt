package ir.platform.x64.regalloc

import ir.value.LocalValue
import asm.x64.Operand
import common.forEachWith
import ir.value.asType
import ir.module.FunctionData
import ir.instruction.Callable
import ir.instruction.Generate
import ir.pass.analysis.intervals.LiveIntervals
import ir.types.NonTrivialType
import ir.types.TupleType


class LinearScan private constructor(private val data: FunctionData, private val liveRanges: LiveIntervals) {
    private val registerMap = hashMapOf<LocalValue, Operand>()
    private val active      = hashMapOf<LocalValue, Operand>()
    private val pool        = VirtualRegistersPool.create(data.arguments())
    private val liveRangesGroup = Precoloring.evaluate(liveRanges)

    init {
        allocRegistersForArgumentValues()
        handleCallArguments()
        handleStackAlloc()
        allocRegistersForLocalVariables()
    }

    private fun build(): RegisterAllocation {
        return RegisterAllocation(pool.stackSize(), registerMap, liveRanges)
    }

    private fun allocRegistersForArgumentValues() {
        for (arg in data.arguments()) {
            registerMap[arg] = pool.takeArgument(arg)
        }
    }

    private fun handleCallArguments() {
       for (bb in data) {
           val inst = bb.last()
           if (inst !is Callable) {
               continue
           }
           val allocation = pool.calleeArgumentAllocate(inst.arguments())
           allocation.forEachWith(inst.arguments()) { operand, arg ->
               registerMap[arg as LocalValue] = operand
           }
       }
    }

    private fun handleStackAlloc() {
        for ((value , _) in liveRanges) {
            if (value !is Generate) {
                continue
            }

            val operand = pool.allocSlot(value)
            registerMap[value] = operand
            active[value] = operand
        }
    }

    private fun allocRegistersForLocalVariables() {
        for ((group, range) in liveRangesGroup) {
            if (registerMap[group.first()] != null) {
                continue
            }
            if (group.first().type() is TupleType) {
                // Skip tuple instructions
                // Register allocation for tuple instructions will be done for their projections
                continue
            }

            active.entries.retainAll {
                if (liveRangesGroup[it.key].end() <= range.begin()) {
                    val size = it.key.asType<NonTrivialType>().sizeOf()
                    pool.free(it.value, size)
                    return@retainAll false
                } else {
                    return@retainAll true
                }
            }
            pickOperandGroup(group)
        }
    }

    private fun pickOperandGroup(group: Group) {
        val operand = pool.allocSlot(group.first())
        for (value in group) {
            registerMap[value] = operand
            active[value] = operand
        }
    }

    override fun toString(): String {
        val builder = StringBuilder()
        builder.append("----Liveness----\n")
            .append(liveRanges.toString())
            .append("----Groups----\n")
            .append(liveRangesGroup.toString())
            .append("----Register allocation----\n")

        for ((group, _) in liveRangesGroup) {
            for (value in group) {
                builder.append("$value -> ${registerMap[value]}\n")
            }
        }
        builder.append("----The end----\n")
        return builder.toString()
    }

    companion object {
        fun alloc(data: FunctionData, liveRanges: LiveIntervals): RegisterAllocation {
            val linearScan = LinearScan(data, liveRanges)
            return linearScan.build()
        }
    }
}