package ir.pass.common

import ir.module.FunctionData
import ir.module.Module
import ir.module.Sensitivity
import ir.pass.CompileContext


abstract class TransformPassFabric {
    abstract fun create(module: Module, ctx: CompileContext): TransformPass
}

abstract class FunctionAnalysisPassFabric<out T: AnalysisResult> {
    abstract fun type(): AnalysisType
    abstract fun sensitivity(): Sensitivity
    abstract fun create(functionData: FunctionData): T
}