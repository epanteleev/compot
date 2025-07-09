package ir.pass.common

import ir.module.FunctionData
import ir.module.Module
import ir.module.Sensitivity
import ir.pass.CompileContext


abstract class TransformPassFabric<M: Module<*>> {
    abstract fun create(module: M, ctx: CompileContext): TransformPass<M>
}

abstract class FunctionAnalysisPassFabric<out T: AnalysisResult> {
    abstract fun type(): AnalysisType
    abstract fun sensitivity(): Sensitivity
    abstract fun create(functionData: FunctionData): T
}