package ir.platform.x64.auxiliary

import ir.module.FunctionData
import ir.platform.x64.LModule

//TODO copy-pasted from DumpModule.kt, should be refactored
internal open class DumpLModule(private val module: LModule){
    protected val builder = StringBuilder()

    private fun dump() {
        dumpExternFunctions()
        dumpTypes()
        dumpConstants()
        dumpGlobals()
        for ((i, fn) in module.functions().withIndex()) {
            builder.append(functionDump(fn))
            if (i != module.functions().size - 1) {
                builder.append('\n')
            }
        }
    }

    private fun dumpExternFunctions() {
        for (fn in module.externFunctions.values) {
            builder.append(fn)
            builder.append('\n')
        }
        if (module.externFunctions.isNotEmpty()) {
            builder.append('\n')
        }
    }

    private fun dumpConstants() {
        for (c in module.constantPool.values) {
            builder.append(c.dump())
            builder.append('\n')
        }
        if (module.constantPool.isNotEmpty()) {
            builder.append('\n')
        }
    }

    private fun dumpGlobals() {
        for (global in module.globals.values) {
            builder.append(global.dump())
            builder.append('\n')
        }
        if (module.globals.isNotEmpty()) {
            builder.append('\n')
        }
    }

    private fun dumpTypes() {
        for (structType in module.types.values) {
            builder.append(structType.dump())
            builder.append('\n')
        }
        if (module.types.isNotEmpty()) {
            builder.append('\n')
        }
    }

    override fun toString(): String {
        dump()
        return builder.toString()
    }

    fun functionDump(functionData: FunctionData): StringBuilder {
        return DumpLIRFunctionData(functionData).builder()
    }
}