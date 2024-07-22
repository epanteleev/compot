package ir.platform.x64.regalloc

import ir.value.LocalValue
import asm.x64.Operand
import common.forEachWith
import ir.value.asType
import ir.module.FunctionData
import ir.instruction.Callable
import ir.liveness.GroupedLiveIntervals
import ir.liveness.LiveIntervals
import ir.types.NonTrivialType
import ir.types.TupleType


class LinearScan private constructor(private val data: FunctionData, private val liveRanges: LiveIntervals) {
    private val registerMap = hashMapOf<LocalValue, Operand>()
    private val active      = hashMapOf<Group, Operand>()
    private val pool        = VirtualRegistersPool.create(data.arguments())
    private val liveRangesGroup: GroupedLiveIntervals

    init {
        allocRegistersForArgumentValues()
        handleCallArguments()

        liveRangesGroup = Precoloring.evaluate(liveRanges, registerMap)

        handleStackAlloc(liveRangesGroup)
        allocRegistersForLocalVariables(liveRangesGroup)
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
       for (bb in data.blocks) {
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

    private fun handleStackAlloc(liveRangesGroup: GroupedLiveIntervals) {
        for ((group, _) in liveRangesGroup) {
            if (!group.stackAllocGroup) {
                continue
            }

            pickOperandGroup(group)
        }
    }

    private fun allocRegistersForLocalVariables(liveRangesGroup: GroupedLiveIntervals) {
        for ((group, range) in liveRangesGroup) {
            val arg = group.precolored
            if (arg != null || group.stackAllocGroup) {
                continue
            }
            if (group.first().type() is TupleType) {
                // Skip tuple instructions
                // Register allocation for tuple instructions will be done for their projections
                continue
            }

            active.entries.retainAll {
                if (liveRangesGroup[it.key].end() <= range.begin()) {
                    val size = it.key.first().asType<NonTrivialType>().sizeOf()
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
        active[group] = operand
        for (value in group) {
            registerMap[value] = operand
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
        fun alloc(data: FunctionData): RegisterAllocation {
            return alloc(data, data.liveness())
        }

        fun alloc(data: FunctionData, liveRanges: LiveIntervals): RegisterAllocation {
            val linearScan = LinearScan(data, liveRanges)
            return linearScan.build()
        }
    }
}