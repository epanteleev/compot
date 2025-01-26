package ir.module.auxiliary

import ir.module.*


abstract class DumpModule<T: Module> protected constructor(protected val module: T) {
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

    abstract fun functionDump(functionData: FunctionData): StringBuilder

    private fun dumpExternFunctions() {
        for (fn in module.functionDeclarations.values) {
            builder.append(fn)
            builder.append('\n')
        }
        if (module.functionDeclarations.isNotEmpty()) {
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
}

internal class DumpSSAModule(module: SSAModule) : DumpModule<SSAModule>(module) {
    override fun functionDump(functionData: FunctionData): StringBuilder {
        return DumpSSAFunctionData(functionData).builder()
    }
}