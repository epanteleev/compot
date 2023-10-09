package ir.codegen.x64.regalloc

import ir.*
import asm.x64.Operand
import ir.codegen.x64.VirtualRegistersPool

class LinearScan(val data: FunctionData) {
    private val liveRanges = data.liveness()
    private val liveRangesGroup = Coalescing.evaluate(liveRanges)
    private val registerMap = hashMapOf<LocalValue, Operand>()
    private val active = hashMapOf<Group, Operand>()
    private val pool = VirtualRegistersPool()

    init {
        allocRegistersForArguments()
        handleStackAlloc()
        allocRegistersForLocalVariables()
    }

    fun build(): RegisterAllocation {
        val frameSize = pool.stackSize()
        return RegisterAllocation(frameSize, registerMap, liveRanges)
    }

    private fun allocRegistersForArguments() {
        for ((group, _) in liveRangesGroup) {
            val arg = group.hasArgument
                ?: break

            val operand = pool.allocArgument(arg)
            active[group] = operand
            for (value in group) {
                registerMap[value] = operand
            }
        }
    }

    private fun handleStackAlloc() {
        for ((group, _) in liveRangesGroup) {
            if (!group.stackAllocGroup) {
                continue
            }

            pickOperandGroup(group)
        }
    }

    private fun allocRegistersForLocalVariables() {
        for ((group, range) in liveRangesGroup) {
            val arg = group.hasArgument
            if (arg != null || group.stackAllocGroup) {
                continue
            }

            active.entries.removeIf {
                if (liveRangesGroup[it.key].end() < range.begin()) {
                    pool.free(it.value)
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
            val linearScan = LinearScan(data)
            return linearScan.build()
        }
    }
}