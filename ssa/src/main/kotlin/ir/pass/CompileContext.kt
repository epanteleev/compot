package ir.pass

import java.io.File
import ir.module.Module


class CompileContext(private val filename: String, private val suffix: String, private val loggingLevel: Int) {

    fun log(passName: String, message: () -> String) {
        if (loggingLevel == 0) {
            return
        }
        val outputFile = File("$filename/${passName}${suffix}.ir")
        outputFile.writeText(message())
    }
}


class CompileContextBuilder(private val filename: String) {
    private var suffix: String? = null
    private var dumpIr: Boolean = false

    fun setSuffix(name: String): CompileContextBuilder {
        suffix = name
        return this
    }

    fun withDumpIr(isDumpIr: Boolean): CompileContextBuilder {
        dumpIr = isDumpIr
        return this
    }

    fun construct(): CompileContext {
        return CompileContext(filename, suffix ?: "", if (dumpIr) 1 else 0) //TODO: fix this
    }
}