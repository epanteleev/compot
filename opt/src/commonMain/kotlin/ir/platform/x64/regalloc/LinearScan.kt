package ir.platform.x64.regalloc

import ir.value.LocalValue
import asm.x64.Operand
import asm.x64.Register
import common.forEachWith
import ir.value.asType
import ir.module.FunctionData
import ir.instruction.Callable
import ir.instruction.Generate
import ir.pass.analysis.InterferenceGraphFabric
import ir.pass.analysis.intervals.LiveIntervals
import ir.types.NonTrivialType
import ir.types.TupleType


class LinearScan private constructor(private val data: FunctionData, private val liveRanges: LiveIntervals) {
    private val registerMap = hashMapOf<LocalValue, Operand>()
    private val fixedValues = arrayListOf<LocalValue>()

    private val active      = hashMapOf<LocalValue, Operand>()
    private val pool        = VirtualRegistersPool.create(data.arguments())
    private val liveRangesGroup = MergeIntervals.evaluate(data)
    private val interferenceGraph = data.analysis(InterferenceGraphFabric)

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
            val reg = pool.takeArgument(arg)
            fixedValues.add(arg)
            registerMap[arg] = reg
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
               fixedValues.add(arg as LocalValue)
               registerMap[arg] = operand
           }
       }
    }

    private fun handleStackAlloc() {
        for ((value , _) in liveRanges) {
            if (value !is Generate) {
                continue
            }

            val operand = pool.allocSlot(value) { false }
            registerMap[value] = operand
            active[value] = operand
        }
    }

    private fun allocRegistersForLocalVariables() {
        for ((value, range) in liveRanges) {
            if (registerMap[value] != null) {
                continue
            }

            if (value.type() is TupleType) {
                // Skip tuple instructions
                // Register allocation for tuple instructions will be done for their projections
                continue
            }

            active.entries.retainAll {
                if (liveRanges[it.key].end() <= range.begin()) {
                    val size = it.key.asType<NonTrivialType>().sizeOf()
                    pool.free(it.value, size)
                    return@retainAll false
                } else {
                    return@retainAll true
                }
            }
            pickOperandGroup(value)
        }
    }

    private fun excludeIf(neighbors: Set<LocalValue>?, reg: Register): Boolean {
        if (neighbors == null) {
            return false
        }
        return neighbors.any { registerMap[it] == reg }
    }

    private fun pickOperandGroup(value: LocalValue) {
        val group = liveRangesGroup.getGroup(value)
        val neighbors = interferenceGraph.neighbors(value)
        if (group == null) {
            val operand = pool.allocSlot(value) { reg -> excludeIf(neighbors, reg) }
            registerMap[value] = operand
            active[value] = operand
            return
        }

        val operand = pool.allocSlot(group.first()) { reg -> excludeIf(neighbors, reg) }
        for (v in group) {
            registerMap[v] = operand
            active[v] = operand
        }
    }

    override fun toString(): String {
        val builder = StringBuilder()
        builder.append("----Liveness----\n")
            .append(liveRanges.toString())
            .append("----Groups----\n")
            .append(liveRangesGroup.toString())
            .append("----Register allocation----\n")

        for ((value , _) in liveRanges) {
            builder.append("$value -> ${registerMap[value]}\n")
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