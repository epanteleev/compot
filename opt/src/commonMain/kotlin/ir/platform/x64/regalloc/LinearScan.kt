package ir.platform.x64.regalloc

import ir.value.LocalValue
import asm.x64.Operand
import asm.x64.Register
import common.assertion
import common.forEachWith
import ir.value.asType
import ir.module.FunctionData
import ir.instruction.Callable
import ir.pass.analysis.InterferenceGraphFabric
import ir.pass.analysis.intervals.LiveIntervalsFabric
import ir.types.NonTrivialType
import ir.types.TupleType


class LinearScan private constructor(private val data: FunctionData) {
    private val liveRanges = data.analysis(LiveIntervalsFabric)
    private val interferenceGraph = data.analysis(InterferenceGraphFabric)

    private val registerMap = hashMapOf<LocalValue, Operand>()
    private val fixedValues = arrayListOf<LocalValue>()
    private val active      = hashMapOf<LocalValue, Operand>()
    private val pool        = VirtualRegistersPool.create(data.arguments())
    private val liveRangesGroup = MergeIntervals.evaluate(data)

    init {
        allocRegistersForArgumentValues()
        handleCallArguments()
        allocRegistersForLocalVariables()
    }

    private fun build(): RegisterAllocation {
        return RegisterAllocation(pool.stackSize(), registerMap)
    }

    private fun allocRegistersForArgumentValues() {
        for (arg in data.arguments()) {
            fixedValues.add(arg)
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
               fixedValues.add(arg as LocalValue)
               registerMap[arg] = operand
           }
       }
    }

    private fun allocRegistersForLocalVariables() {
        for ((value, range) in liveRanges) {
            if (registerMap[value] != null) {
                assertion(fixedValues.contains(value)
                        || liveRangesGroup.getGroup(value) != null) {
                    "value=$value is already allocated"
                }
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
        val operand = pool.allocSlot(value) { reg -> excludeIf(neighbors, reg) }
        if (group == null) {
            registerMap[value] = operand
            active[value] = operand
            return
        }
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
        fun alloc(data: FunctionData): RegisterAllocation {
            val linearScan = LinearScan(data)
            return linearScan.build()
        }
    }
}