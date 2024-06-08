package ir.pass

import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM


class CompileContext(private val filename: String, private val suffix: String, private val outputDir: String?) {

    fun log(passName: String, message: () -> String) {
        if (outputDir == null) {
            return
        }

        val outputFile = "${outputDir}/$filename/${passName}${suffix}.ir".toPath()
        FileSystem.SYSTEM.write(outputFile) {
            writeUtf8(message())
        }
    }
}


class CompileContextBuilder(private val filename: String) {
    private var suffix: String? = null
    private var dumpIr: String? = null

    fun setSuffix(name: String): CompileContextBuilder {
        suffix = name
        return this
    }

    fun withDumpIr(outputDir: String): CompileContextBuilder {
        dumpIr = outputDir
        return this
    }

    fun construct(): CompileContext {
        return CompileContext(filename, suffix ?: "", dumpIr) //TODO: fix this
    }
}