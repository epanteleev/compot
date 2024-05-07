package ir.pass.transform

import ir.module.Module
import ir.pass.PassFabric
import ir.pass.TransformPass
import ir.platform.x64.LModule
import ir.pass.transform.auxiliary.*
import ir.platform.x64.pass.transform.*


class SSADestruction(module: Module): TransformPass(module) {
    override fun name(): String = "ssa-destruction"
    override fun run(): Module {
        val transformed = AllocLoadStoreReplacement.run(
            MoveLargeConstants.run(
                ReplaceFloatNeg.run(
                    FunctionsIsolation.run(module))
            )
        )

        return LModule(transformed.functions, transformed.externFunctions, transformed.globals, transformed.types)
    }
}

object SSADestructionFabric: PassFabric {
    override fun create(module: Module): TransformPass {
        return SSADestruction(module.copy())
    }
}