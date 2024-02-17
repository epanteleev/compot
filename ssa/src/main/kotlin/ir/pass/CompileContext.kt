package ir.pass

import java.io.File
import ir.module.Module


class CompileContext(private val filename: String, private val suffix: String, private val loggingLevel: Int) {
    fun log(passName: String, pass: Module) {
        if (loggingLevel == 0) {
            return
        }
        val outputFile = File("$filename/${passName}${suffix}.ir")
        outputFile.writeText(pass.toString())
    }
}

class CompileContextBuilder(private val filename: String) {
    private var suffix: String? = null
    private var loggingLevel: Int = 0

    fun setSuffix(name: String): CompileContextBuilder {
        suffix = name
        return this
    }

    fun setLoggingLevel(level: Int): CompileContextBuilder {
        loggingLevel = level
        return this
    }

    fun construct(): CompileContext {
        return CompileContext(filename, suffix ?: "", loggingLevel)
    }
}