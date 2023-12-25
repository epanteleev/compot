package ir.platform.x64

import ir.*
import ir.module.*
import ir.module.auxiliary.*
import ir.platform.liveness.LiveIntervals
import ir.platform.regalloc.LinearScan
import ir.platform.regalloc.RegisterAllocation

data class CSSAModule(override val functions: List<FunctionData>,
                      override val externFunctions: Set<ExternFunction>,
                      override val constants: Set<GlobalValue>):
    Module(functions, externFunctions, constants) {
    private val liveIntervals: Map<FunctionData, LiveIntervals>
    private val registerAllocation: Map<FunctionData, RegisterAllocation>

    init {
        liveIntervals = hashMapOf()
        for (fn in functions) {
            liveIntervals[fn] = fn.liveness()
        }

        registerAllocation = hashMapOf()
        for ((fn, liveIntervals) in liveIntervals) {
            registerAllocation[fn] = LinearScan.alloc(fn, liveIntervals)
        }
    }

    fun regAlloc(data: FunctionData): RegisterAllocation {
        val allocation = registerAllocation[data]
        assert(allocation != null) {
            "cannot find register allocation information for ${data.prototype}"
        }

        return allocation!!
    }

    fun liveInfo(data: FunctionData): LiveIntervals {
        val liveInfo = liveIntervals[data]
        assert(liveInfo != null) {
            "cannot liveness information for ${data.prototype}"
        }

        return liveInfo!!
    }

    override fun copy(): Module {
        return SSAModule(functions.map { Copy.copy(it) }, externFunctions, constants)
    }

    override fun toString(): String {
        return DumpModule.dump(this)
    }
}