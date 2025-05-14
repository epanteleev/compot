package ir.pass

import java.nio.file.Path


sealed interface CompileContext{
    fun pic(): Boolean
    fun outputFile(passName: String): Path?

    companion object {
         fun empty(): CompileContext {
             return CompileContextImpl("", "", null, false)
         }
    }
}

class CompileContextImpl(private val filename: String, private val suffix: String, private val outputDir: String?, val picEnabled: Boolean): CompileContext {
    override fun outputFile(passName: String): Path? {
        if (outputDir == null) {
            return null
        }

        return Path.of("${outputDir}/$filename/${passName}${suffix}.ir")
    }

    override fun pic(): Boolean {
        return picEnabled
    }
}

class CompileContextBuilder(private val filename: String) {
    private var suffix: String? = null
    private var dumpIr: String? = null
    private var picEnabled: Boolean = false

    fun setSuffix(name: String): CompileContextBuilder {
        suffix = name
        return this
    }

    fun withDumpIr(outputDir: String): CompileContextBuilder {
        dumpIr = outputDir
        return this
    }

    fun setPic(picEnabled: Boolean): CompileContextBuilder {
        this.picEnabled = picEnabled
        return this
    }

    fun construct(): CompileContext {
        return CompileContextImpl(filename, suffix ?: "", dumpIr, picEnabled) //TODO: fix this
    }
}