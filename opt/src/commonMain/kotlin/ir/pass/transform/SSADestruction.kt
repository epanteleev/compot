package ir.pass.transform

import ir.module.SSAModule
import ir.pass.CompileContext
import ir.pass.common.TransformPassFabric
import ir.pass.common.TransformPass
import ir.pass.transform.auxiliary.*
import ir.platform.x64.auxiliary.Lowering


class SSADestruction(module: SSAModule, ctx: CompileContext): TransformPass<SSAModule>(module, ctx) {
    override fun name(): String = "ssa-destruction"
    override fun run(): SSAModule {
        val transformed = Lowering.run(FunctionsIsolation.run(module, ctx), ctx)
        return SSAModule(transformed.functions, transformed.externFunctions, transformed.constantPool, transformed.globals, transformed.types)
    }
}

object SSADestructionFabric: TransformPassFabric<SSAModule>() {
    override fun create(module: SSAModule, ctx: CompileContext): TransformPass<SSAModule> = SSADestruction(module.copy(), ctx)
}