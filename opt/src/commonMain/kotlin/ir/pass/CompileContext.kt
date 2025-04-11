package ir.pass

import java.nio.file.Path


class CompileContext(private val filename: String, private val suffix: String, private val outputDir: String?) {
    fun outputFile(passName: String): Path? {
        if (outputDir == null) {
            return null
        }

        return Path.of("${outputDir}/$filename/${passName}${suffix}.ir")
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