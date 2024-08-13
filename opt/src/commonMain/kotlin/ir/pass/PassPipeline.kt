package ir.pass

import ir.module.Module
import ir.pass.analysis.VerifySSA
import ir.pass.common.TransformPassFabric
import ir.pass.transform.CSSAConstructionFabric
import ir.pass.transform.Mem2RegFabric
import ir.pass.transform.SSADestructionFabric
import ir.pass.transform.SwitchReplacementFabric


class PassPipeline private constructor(private val passFabrics: List<TransformPassFabric>, private val ctx: CompileContext) {
    fun run(start: Module): Module {
        var current = start
        ctx.log("initial") { current.toString() }
        for (fabric in passFabrics) {
            try {
                val pass = fabric.create(current)
                current = pass.run()
                VerifySSA.run(current)
                ctx.log(pass.name()) { current.toString() }
            } catch (ex: Throwable) {
                println(current.toString())
                throw ex
            }
        }
        return current
    }

    companion object {
        fun base(ctx: CompileContext): PassPipeline = create(arrayListOf(SwitchReplacementFabric, CSSAConstructionFabric, SSADestructionFabric), ctx)
        fun opt(ctx: CompileContext): PassPipeline = create(arrayListOf(Mem2RegFabric, SwitchReplacementFabric, CSSAConstructionFabric, SSADestructionFabric), ctx)

        fun create(passFabrics: List<TransformPassFabric>, ctx: CompileContext): PassPipeline {
            return PassPipeline(passFabrics, ctx)
        }
    }
}