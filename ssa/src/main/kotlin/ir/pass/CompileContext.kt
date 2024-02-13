package ir.pass

import java.io.File
import ir.module.Module

class CompileContext(private val filename: String, val suffix: String) {
    fun log(passName: String, pass: Module) {
        val outputFile = File("$filename/${passName}${suffix}.ir")
        outputFile.writeText(pass.toString())
    }
}

class CompileContextBuilder(private val filename: String) {
    private var suffix: String? = null

    fun setSuffix(name: String) {
        suffix = name
    }

    fun construct(): CompileContext {
        return CompileContext(filename, suffix ?: "")
    }
}