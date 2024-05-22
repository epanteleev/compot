package ir.platform.x64.regalloc

import ir.LocalValue
import asm.x64.Operand
import common.forEachWith
import ir.asType
import ir.module.FunctionData
import ir.instruction.Callable
import ir.instruction.TupleInstruction
import ir.liveness.GroupedLiveIntervals
import ir.liveness.LiveIntervals
import ir.types.NonTrivialType


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
           val allocation = CalleeArgumentAllocator.alloc(inst.arguments().toList()) //TODO allocation
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
            if (group.first() is TupleInstruction) {
                // Skip tuple instructions
                // Register allocation for tuple instructions will be done for their projections
                continue
            }

            active.entries.removeIf {
                if (liveRangesGroup[it.key].end() <= range.begin()) {
                    pool.free(it.value, it.key.first().asType<NonTrivialType>().size())
                    return@removeIf true
                } else {
                    return@removeIf false
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