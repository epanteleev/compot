package ir.pass.transform

import ir.module.Module
import ir.pass.CompileContext
import ir.pass.common.TransformPassFabric
import ir.pass.common.TransformPass
import ir.platform.x64.LModule
import ir.pass.transform.auxiliary.*
import ir.platform.x64.auxiliary.Lowering


class SSADestruction(module: Module, ctx: CompileContext): TransformPass(module, ctx) {
    override fun name(): String = "ssa-destruction"
    override fun run(): Module {
        val transformed = Lowering.run(FunctionsIsolation.run(module), ctx)
        return LModule(transformed.functions, transformed.functionDeclarations, transformed.constantPool, transformed.globals, transformed.types)
    }
}

object SSADestructionFabric: TransformPassFabric() {
    override fun create(module: Module, ctx: CompileContext): TransformPass = SSADestruction(module.copy(), ctx)
}