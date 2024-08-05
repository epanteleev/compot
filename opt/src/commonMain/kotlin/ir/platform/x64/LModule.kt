package ir.platform.x64

import common.assertion
import ir.global.*
import ir.module.*
import ir.types.StructType
import ir.module.auxiliary.*
import ir.platform.x64.regalloc.LinearScan
import ir.platform.x64.regalloc.RegisterAllocation
import ir.pass.analysis.intervals.LiveIntervals
import ir.pass.analysis.intervals.LiveIntervalsBuilder
import ir.pass.analysis.intervals.LiveIntervalsFabric


class LModule(functions: List<FunctionData>,
              externFunctions: Map<String, ExternFunction>,
              constantPool: Map<String, GlobalConstant>,
              globals: Map<String, GlobalValue>,
              types: Map<String, StructType>):
    Module(functions, externFunctions, constantPool, globals, types) {
    private val liveIntervals: Map<FunctionData, LiveIntervals>
    private val registerAllocation: Map<FunctionData, RegisterAllocation>

    init {
        liveIntervals = hashMapOf()
        for (fn in functions) {
            liveIntervals[fn] = fn.analysis(LiveIntervalsFabric)
        }

        registerAllocation = hashMapOf()
        for ((fn, liveIntervals) in liveIntervals) {
            registerAllocation[fn] = LinearScan.alloc(fn, liveIntervals)
        }
    }

    fun regAlloc(data: FunctionData): RegisterAllocation {
        val allocation = registerAllocation[data]
        assertion(allocation != null) {
            "cannot find register allocation information for ${data.prototype}"
        }

        return allocation!!
    }

    fun liveInfo(data: FunctionData): LiveIntervals {
        val liveInfo = liveIntervals[data]
        assertion(liveInfo != null) {
            "cannot liveness information for ${data.prototype}"
        }

        return liveInfo!!
    }

    override fun copy(): Module {
        return LModule(functions.map { CopyCFG.copy(it) }, externFunctions, constantPool, globals, types) //TODO deep copy???
    }

    override fun toString(): String {
        return DumpModule.dump(this)
    }
}