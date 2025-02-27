package ir.pass

import ir.module.Module
import ir.pass.analysis.VerifySSA
import ir.pass.common.TransformPassFabric
import ir.pass.transform.Mem2RegFabric
import okio.FileSystem


class PassPipeline private constructor(private val passFabrics: List<TransformPassFabric>, private val ctx: CompileContext) {
    fun run(start: Module): Module {
        var current = start
        dumpIr("initial") { current.toString() }
        for (fabric in passFabrics) {
            try {
                val pass = fabric.create(current)
                current = pass.run()
                VerifySSA.run(current)
                dumpIr(pass.name()) { current.toString() }
            } catch (ex: Throwable) {
                println(current.toString())
                throw ex
            }
        }
        return current
    }

    private fun dumpIr(passName: String, message: () -> String) {
        val filename = ctx.outputFile(passName) ?: return

        FileSystem.SYSTEM.write(filename) {
            writeUtf8(message())
        }
    }

    companion object {
        fun base(ctx: CompileContext): PassPipeline = create(arrayListOf(), ctx)
        fun opt(ctx: CompileContext): PassPipeline = create(arrayListOf(Mem2RegFabric), ctx)

        fun create(passFabrics: List<TransformPassFabric>, ctx: CompileContext): PassPipeline {
            return PassPipeline(passFabrics, ctx)
        }
    }
}