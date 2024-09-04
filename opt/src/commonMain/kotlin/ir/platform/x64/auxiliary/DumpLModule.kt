package ir.platform.x64.auxiliary

import ir.module.FunctionData
import ir.module.auxiliary.DumpModule
import ir.platform.x64.LModule


internal class DumpLModule(module: LModule) : DumpModule<LModule>(module) {
    override fun functionDump(functionData: FunctionData): StringBuilder {
        return DumpLIRFunctionData(functionData).builder()
    }
}