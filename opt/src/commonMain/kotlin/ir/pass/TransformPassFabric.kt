package ir.pass

import ir.module.FunctionData
import ir.module.Module


interface TransformPassFabric {
    fun create(module: Module): TransformPass
}

interface FunctionAnalysisPassFabric {
    fun create(functionData: FunctionData): FunctionAnalysisPass
}