package ir.platform.x64

import common.assertion
import ir.global.*
import ir.module.*
import ir.types.StructType
import ir.module.auxiliary.*
import ir.platform.x64.regalloc.LinearScan
import ir.platform.x64.regalloc.RegisterAllocation
import ir.pass.analysis.intervals.LiveIntervals
import ir.pass.analysis.intervals.LiveIntervalsFabric


class LModule(functions: Map<String, FunctionData>,
              externFunctions: Map<String, ExternFunction>,
              constantPool: Map<String, GlobalConstant>,
              globals: Map<String, AnyGlobalValue>,
              types: Map<String, StructType>):
    Module(functions, externFunctions, constantPool, globals, types) {
    private val liveIntervals: Map<FunctionData, LiveIntervals>
    private val registerAllocation: Map<FunctionData, RegisterAllocation>

    init {
        liveIntervals = hashMapOf()
        for (fn in functions.values) {
            liveIntervals[fn] = fn.analysis(LiveIntervalsFabric)
        }

        registerAllocation = hashMapOf()
        for (fn in functions.values) {
            registerAllocation[fn] = LinearScan.alloc(fn)
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
        val newMap = hashMapOf<String, FunctionData>()
        for ((name, function) in functions) {
            newMap[name] = function.copy()
        }
        return LModule(newMap, externFunctions, constantPool, globals, types) //TODO deep copy???
    }

    override fun toString(): String {
        return DumpModule.dump(this)
    }
}