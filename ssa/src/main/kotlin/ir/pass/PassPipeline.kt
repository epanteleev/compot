package ir.pass

import ir.module.Module
import ir.pass.ana.VerifySSA
import kotlin.system.exitProcess
import ir.pass.transform.Mem2RegFabric
import ir.pass.transform.SSADestructionFabric


class PassPipeline private constructor(private val passFabrics: List<PassFabric>, private val ctx: CompileContext) {
    fun run(start: Module): Module {
        var current = start
        for (fabric in passFabrics) {
            try {
                val pass = fabric.create(current)
                current = pass.run()
                VerifySSA.run(current)
                ctx.log(pass.name()) { current.toString() }
            } catch (ex: Throwable) {
                println(current.toString())
                ex.printStackTrace()
                exitProcess(1)
            }
        }
        return current
    }

    companion object {
        fun base(ctx: CompileContext): PassPipeline = create(arrayListOf(SSADestructionFabric), ctx)
        fun opt(ctx: CompileContext): PassPipeline = create(arrayListOf(Mem2RegFabric, SSADestructionFabric), ctx)

        fun create(passFabrics: List<PassFabric>, ctx: CompileContext): PassPipeline {
            return PassPipeline(passFabrics, ctx)
        }
    }
}