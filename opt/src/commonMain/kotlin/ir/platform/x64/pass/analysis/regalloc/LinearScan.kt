package ir.platform.x64.pass.analysis.regalloc

import ir.types.*
import asm.Operand
import asm.Register
import ir.value.LocalValue
import asm.x64.GPRegister.rcx
import asm.x64.GPRegister.rdx
import common.assertion
import common.forEachWith
import ir.value.asType
import ir.module.FunctionData
import ir.instruction.Callable
import ir.instruction.Copy
import ir.instruction.Generate
import ir.instruction.Instruction
import ir.instruction.Projection
import ir.instruction.Shl
import ir.instruction.Shr
import ir.instruction.TupleDiv
import ir.instruction.lir.Lea
import ir.instruction.match
import ir.instruction.matching.*
import ir.module.Sensitivity
import ir.module.block.Block
import ir.pass.analysis.InterferenceGraphFabric
import ir.pass.analysis.intervals.LiveIntervalsFabric
import ir.pass.common.AnalysisType
import ir.pass.common.FunctionAnalysisPass
import ir.pass.common.FunctionAnalysisPassFabric
import ir.value.asValue


class LinearScan internal constructor(private val data: FunctionData): FunctionAnalysisPass<RegisterAllocation>() {
    private val liveRanges = data.analysis(LiveIntervalsFabric)
    private val interferenceGraph = data.analysis(InterferenceGraphFabric)

    private val registerMap = hashMapOf<LocalValue, Operand>()
    private val callInfo    = hashMapOf<Callable, List<Operand?>>()
    private val active      = hashMapOf<LocalValue, Operand>()
    private val pool        = VirtualRegistersPool.create(data.arguments())

    init {
        allocFixedRegisters()
        allocRegistersForArgumentValues()
        allocRegistersForLocalVariables()
    }

    override fun run(): RegisterAllocation {
        return RegisterAllocation(pool.spilledLocalsAreaSize(), registerMap, pool.usedGPCalleeSaveRegisters(), callInfo, data.marker())
    }

    private fun allocate(slot: Operand, value: LocalValue) {
        registerMap[value] = slot
        active[value] = slot
    }

    private fun allocRegistersForArgumentValues() {
        for (arg in data.arguments()) {
            allocate(pool.takeArgument(arg), arg)
        }
    }

    private fun allocFunctionArguments(callable: Callable) {
        val allocation = pool.calleeArgumentAllocate(callable.arguments())
        callInfo[callable] = allocation

        allocation.forEachWith(callable.arguments()) { operand, arg ->
            if (operand == null) {
                // Nothing to do. UB happens
                return@forEachWith
            }
            assertion(arg is Copy || arg is Lea || arg is Generate) { "arg=$arg" }

            registerMap[arg as LocalValue] = operand
        }
    }

    private fun tryAllocFixedRegisters(bb: Block) {
        fun visitor(inst: Instruction) {
            inst.match(shl(nop(), constant().not())) { shl: Shl ->
                assertion(shl.rhs() is Copy) { "shl=$shl" }
                registerMap[shl.rhs().asValue()] = rcx
                return
            }

            inst.match(shr(nop(), constant().not())) { shr: Shr ->
                assertion(shr.rhs() is Copy) { "shr=$shr" }
                registerMap[shr.rhs().asValue()] = rcx
                return
            }

            inst.match(tupleDiv(nop(), nop())) { div: TupleDiv ->
                val rem = div.remainder()
                assertion(rem != null) { "div=$div" }
                registerMap[rem as Projection] = rdx
                return
            }
        }
        bb.forEach { visitor(it) }
    }

    private fun allocFixedRegisters() {
       for (bb in data) {
           val inst = bb.last()
           if (inst is Callable) {
               allocFunctionArguments(inst)
           }

           tryAllocFixedRegisters(bb)
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
                // Found fixed register. Skip it
                // Register allocation for fixed registers is already done
                active[value] = reg
                continue
            }

            active.entries.retainAll { (local, operand) ->
                if (liveRanges[local].end().to <= range.begin().from) {
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

    private fun excludeIf(value: LocalValue, reg: Register): Boolean {
        val neighbors = interferenceGraph.neighbors(value) ?: return false
        return neighbors.any { registerMap[it] == reg }
    }

    private fun pickOperandGroup(value: LocalValue) {
        val operand = pool.allocSlot(value) { reg -> excludeIf(value, reg) }
        val group = liveRanges.getGroup(value) ?: return allocate(operand, value)
        for (v in group) {
            allocate(operand, v)
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