package ir.platform.x64.pass.analysis.regalloc

import ir.value.LocalValue
import asm.x64.Operand
import asm.x64.Register
import common.assertion
import common.forEachWith
import ir.value.asType
import ir.module.FunctionData
import ir.instruction.Callable
import ir.module.Sensitivity
import ir.pass.analysis.InterferenceGraphFabric
import ir.pass.analysis.intervals.LiveIntervalsFabric
import ir.pass.common.AnalysisType
import ir.pass.common.FunctionAnalysisPass
import ir.pass.common.FunctionAnalysisPassFabric
import ir.types.NonTrivialType
import ir.types.TupleType


class LinearScan internal constructor(private val data: FunctionData): FunctionAnalysisPass<RegisterAllocation>() {
    private val liveRanges = data.analysis(LiveIntervalsFabric)
    private val interferenceGraph = data.analysis(InterferenceGraphFabric)

    private val registerMap = hashMapOf<LocalValue, Operand>()
    private val fixedValues = arrayListOf<LocalValue>()
    private val active      = hashMapOf<LocalValue, Operand>()
    private val pool        = VirtualRegistersPool.create(data.arguments())

    init {
        allocRegistersForArgumentValues()
        handleCallArguments()
        allocRegistersForLocalVariables()
    }

    override fun run(): RegisterAllocation {
        return RegisterAllocation(pool.stackSize(), registerMap, data.marker())
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
                        || liveRanges.getGroup(value) != null) {
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
        val group = liveRanges.getGroup(value)
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
            .append("----Register allocation----\n")

        for ((value , _) in liveRanges) {
            builder.append("$value -> ${registerMap[value]}\n")
        }
        builder.append("----The end----\n")
        return builder.toString()
    }
}

object LinearScanFabric: FunctionAnalysisPassFabric<RegisterAllocation>() {
    override fun type(): AnalysisType {
        return AnalysisType.LINEAR_SCAN
    }

    override fun sensitivity(): Sensitivity {
        return Sensitivity.CONTROL_AND_DATA_FLOW
    }

    override fun create(functionData: FunctionData): RegisterAllocation {
        return LinearScan(functionData).run()
    }
}