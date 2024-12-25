package ir.pass.transform

import ir.module.Module
import ir.pass.common.TransformPassFabric
import ir.pass.common.TransformPass
import ir.platform.x64.LModule
import ir.pass.transform.auxiliary.*
import ir.platform.x64.pass.transform.*


class SSADestruction(module: Module): TransformPass(module) {
    override fun name(): String = "ssa-destruction"
    override fun run(): Module {
        val transformed = ConstantLoading.run(
            MoveLargeConstants.run(Lowering.run(ReplaceByteOperands.run(ReplaceFloatNeg.run(FunctionsIsolation.run(module)))))
        )
        return LModule(transformed.functions, transformed.externFunctions, transformed.constantPool, transformed.globals, transformed.types)
    }
}

object SSADestructionFabric: TransformPassFabric() {
    override fun create(module: Module): TransformPass {
        return SSADestruction(module.copy())
    }
}