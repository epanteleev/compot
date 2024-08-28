package ir.pass

import ir.module.FunctionData
import ir.module.Module


abstract class TransformPassFabric {
    abstract fun create(module: Module): TransformPass
}

abstract class FunctionAnalysisPassFabric<T: AnalysisResult> {
    abstract fun create(functionData: FunctionData): FunctionAnalysisPass<T>
}