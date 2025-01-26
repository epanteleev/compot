package ir.pass.transform

import ir.module.Module
import ir.module.SSAModule
import ir.pass.common.TransformPassFabric
import ir.pass.common.TransformPass
import ir.pass.transform.auxiliary.CopyInsertion
import ir.pass.transform.auxiliary.SplitCriticalEdge


class CSSAConstruction internal constructor(module: Module): TransformPass(module) {
    override fun name(): String = "cssa-construction"

    override fun run(): Module {
        val transformed = CopyInsertion.run(SplitCriticalEdge.run(module))
        return SSAModule(transformed.functions, transformed.functionDeclarations, transformed.constantPool, transformed.globals, transformed.types)
    }
}

object CSSAConstructionFabric: TransformPassFabric() {
    override fun create(module: Module): TransformPass {
        return CSSAConstruction(module.copy())
    }
}