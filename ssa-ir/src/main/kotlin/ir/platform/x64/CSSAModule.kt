package ir.platform.x64

import ir.module.*
import ir.ExternFunction
import ir.module.auxiliary.Copy
import ir.module.auxiliary.DumpModule
import ir.platform.regalloc.LinearScan
import ir.platform.regalloc.RegisterAllocation
import ir.platform.liveness.LiveIntervals

data class CSSAModule(override val functions: List<FunctionData>, override val externFunctions: Set<ExternFunction>):
    Module(functions, externFunctions) {

    private val liveIntervals by lazy {
        val map = hashMapOf<FunctionData, LiveIntervals>()
        for (fn in functions) {
            map[fn] = fn.liveness()
        }
        map
    }

    private val registerAllocation by lazy {
        val map = hashMapOf<FunctionData, RegisterAllocation>()
        for ((fn, liveIntervals) in liveIntervals) {
            map[fn] = LinearScan.alloc(fn, liveIntervals)
        }

        map
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
        return SSAModule(functions.map { Copy.copy(it) }, externFunctions.mapTo(mutableSetOf()) { it })
    }

    override fun toString(): String {
        return DumpModule.dump(this)
    }
}