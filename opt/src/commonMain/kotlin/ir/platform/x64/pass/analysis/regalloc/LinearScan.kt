package ir.platform.x64.pass.analysis.regalloc

import asm.x64.Address
import ir.types.*
import asm.x64.Register
import ir.value.LocalValue
import asm.x64.GPRegister.rcx
import asm.x64.GPRegister.rdx
import asm.x64.VReg
import common.assertion
import common.forEachWith
import ir.Definitions
import ir.Definitions.POINTER_SIZE
import ir.Definitions.QWORD_SIZE
import ir.instruction.*
import ir.value.asType
import ir.module.FunctionData
import ir.instruction.lir.Generate
import ir.instruction.lir.Lea
import ir.module.Sensitivity
import ir.pass.analysis.intervals.LiveIntervalsFabric
import ir.pass.analysis.intervals.LiveRange
import ir.pass.common.AnalysisType
import ir.pass.common.FunctionAnalysisPass
import ir.pass.common.FunctionAnalysisPassFabric
import ir.platform.x64.codegen.CodegenException
import ir.platform.x64.pass.analysis.FixedRegisterInstructionsAnalysis


private class LinearScan(private val data: FunctionData): FunctionAnalysisPass<RegisterAllocation>() {
    private val liveRanges = data.analysis(LiveIntervalsFabric)
    private val fixedRegistersInfo = FixedRegisterInstructionsAnalysis.run(data)

    private val registerMap = hashMapOf<LocalValue, VReg>()
    private val overFlowAreaSize = hashMapOf<Callable, Int>()
    private val active      = linkedMapOf<LocalValue, VReg>()
    private val pool        = VirtualRegistersPool.create(data.arguments())

    private val activeFixedIntervals = arrayListOf<Pair<LiveRange, VReg>>()

    init {
        allocFixedRegisters()
        allocRegistersForArgumentValues()
        allocRegistersForLocalVariables()
    }

    private fun overflowAreaSize(call: Callable, list: List<VReg?>): Int {
        var argumentsSlotsSize = 0
        for ((idx, reg) in list.withIndex()) { // TODO refactor
            if (reg !is Address) {
                continue
            }
            val prototype = call.prototype()
            val byVal = prototype.byValue(idx)
            if (byVal == null) {
                argumentsSlotsSize += POINTER_SIZE
                continue
            }

            val type = prototype.argument(idx) ?: throw CodegenException("argument type is null")
            assertion(type is AggregateType) { "type=$type" }

            argumentsSlotsSize += Definitions.alignTo(type.sizeOf(), QWORD_SIZE)
        }

        return argumentsSlotsSize
    }

    override fun run(): RegisterAllocation {
        return RegisterAllocation(
            pool.spilledLocalsAreaSize(),
            registerMap,
            pool.usedGPCalleeSaveRegisters(),
            overFlowAreaSize,
            data.marker()
        )
    }

    private fun allocate(slot: VReg, value: LocalValue) {
        registerMap[value] = slot
        active[value] = slot
    }

    private fun allocRegistersForArgumentValues() {
        for (arg in data.arguments()) {
            allocate(pool.takeArgument(arg), arg)
        }
    }

    private fun allocFunctionArguments(callable: Callable) {
        val allocation = pool.callerArgumentAllocate(callable.arguments())
        overFlowAreaSize[callable] = overflowAreaSize(callable, allocation)
        allocation.forEachWith(callable.arguments()) { operand, arg ->
            if (operand == null) {
                // Nothing to do. UB happens
                return@forEachWith
            }
            assertion(arg is Copy || arg is Lea || arg is Generate) { "arg=$arg" }

            registerMap[arg as LocalValue] = operand
            activeFixedIntervals.add(liveRanges[arg] to operand)
        }
    }

    private fun allocFixedRegisters() {
        for (bb in data) {
            val inst = bb.last()
            if (inst is Callable) {
                allocFunctionArguments(inst)
            }
        }

        for (value in fixedRegistersInfo.rdxFixedReg) {
            registerMap[value] = rdx
            activeFixedIntervals.add(liveRanges[value] to rdx)
        }

        for (value in fixedRegistersInfo.rcxFixedReg) {
            registerMap[value] = rcx
            activeFixedIntervals.add(liveRanges[value] to rcx)
        }

        activeFixedIntervals.sortedBy { it.first.end() }
    }

    private fun deactivateFixedIntervals(where: LiveRange) {
        activeFixedIntervals.retainAll { (interval, _) ->
            return@retainAll where.begin() <= interval.end()
        }
    }

    private fun allocRegistersForLocalVariables() {
        for ((value, range) in liveRanges) {
            if (value.type() is TupleType) {
                // Skip tuple instructions
                // Register allocation for tuple instructions will be done for their projections
                continue
            }
            if (value.type() is FlagType) {
                // Skip boolean instructions
                continue
            }
            if (value.type() is UndefType) {
                // Skip undefined instructions
                continue
            }
            val reg = registerMap[value]
            if (reg != null) {
                // Found value with fixed register. Skip it
                // Register allocation for such instructions is already done
                active[value] = reg
                continue
            }
            deactivateFixedIntervals(range)

            active.entries.retainAll { (local, operand) ->
                if (!liveRanges[local].intersect(range)) {
                    val size = local.asType<NonTrivialType>().sizeOf()
                    pool.free(operand, size)
                    return@retainAll false
                } else {
                    return@retainAll true
                }
            }
            pickOperandGroup(value)
        }
    }

    private fun excludeIf(value: LocalValue, reg: Register): Boolean { //TODO improve algorithm
        val range = liveRanges[value]
        for ((interval, operand) in activeFixedIntervals) {
            if (interval.intersect(range)) {
                if (operand == reg) {
                    return true
                }
            }

            if (interval.end() < range.begin()) {
                return false
            }
        }

        return false
    }

    private fun pickOperandGroup(value: LocalValue) {
        val operand = pool.allocSlot(value) { reg -> excludeIf(value, reg) }
        val group = liveRanges.getGroup(value) ?: return allocate(operand, value)
        for (v in group) {
            allocate(operand, v)
        }
    }

    override fun toString(): String = buildString {
        append("----Liveness----\n")
        append(liveRanges.toString())
        append("----Register allocation----\n")

        for ((value , _) in liveRanges) {
            append("$value -> ${registerMap[value]}\n")
        }
        append("----The end----\n")
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