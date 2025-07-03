package ir.pass.transform

import ir.module.SSAModule
import ir.pass.CompileContext
import ir.pass.common.TransformPassFabric
import ir.pass.common.TransformPass
import ir.pass.transform.auxiliary.CopyInsertion
import ir.pass.transform.auxiliary.SplitCriticalEdge


class CSSAConstruction internal constructor(module: SSAModule, ctx: CompileContext): TransformPass<SSAModule>(module, ctx) {
    override fun name(): String = "cssa-construction"

    override fun run(): SSAModule {
        val transformed = CopyInsertion.run(SplitCriticalEdge.run(module), ctx)
        return SSAModule(transformed.functions, transformed.externFunctions, transformed.constantPool, transformed.globals, transformed.types)
    }
}

object CSSAConstructionFabric: TransformPassFabric<SSAModule>() {
    override fun create(module: SSAModule, ctx: CompileContext): TransformPass<SSAModule> {
        return CSSAConstruction(module.copy(), ctx)
    }
}