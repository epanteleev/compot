package ir.pass

import ir.module.Module
import ir.pass.analysis.VerifySSA
import ir.pass.common.TransformPassFabric
import ir.pass.transform.DeadCodeElimination
import ir.pass.transform.Mem2RegFabric
import ir.pass.transform.normalizer.Normalizer
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.OutputStreamWriter


class PassPipeline private constructor(private val name: String, private val passFabrics: List<TransformPassFabric>, private val ctx: CompileContext) {
    fun run(start: Module): Module {
        var current = start
        dumpIr(name) { current.toString() }
        for (fabric in passFabrics) {
            try {
                val pass = fabric.create(current, ctx)
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

        if (!filename.parent.toFile().exists()) {
            filename.parent.toFile().mkdirs()
        }
        val outputStream: OutputStream = FileOutputStream(filename.toFile())
        outputStream.use {
            OutputStreamWriter(outputStream).use { it.write(message()) }
        }
    }

    companion object {
        fun base(ctx: CompileContext): PassPipeline = create("initial", arrayListOf(), ctx)
        fun opt(ctx: CompileContext): PassPipeline = create("initial", arrayListOf(Mem2RegFabric, Normalizer, DeadCodeElimination), ctx)

        fun create(name: String, passFabrics: List<TransformPassFabric>, ctx: CompileContext): PassPipeline {
            return PassPipeline(name, passFabrics, ctx)
        }
    }
}