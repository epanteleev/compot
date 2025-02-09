package common

enum class Extension(val value: String) {
    C(".c"),
    IR(".ir"),
    AR(".a"),
    SO(".so"),
    ASM(".s"),
    OBJ(".o"),
    EXE(".out")
}

class ProcessedFile private constructor(val filename: String, val extension: Extension) {
    fun basename(): String {
        return Files.getBasename(filename)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProcessedFile) return false

        if (filename != other.filename) return false
        if (extension != other.extension) return false

        return true
    }

    override fun hashCode(): Int {
        var result = filename.hashCode()
        result = 31 * result + extension.hashCode()
        return result
    }

    override fun toString(): String = filename

    fun withExtension(extension: Extension): ProcessedFile {
        if (this.extension == extension) {
            return this
        }

        return create(basename(), extension)
    }

    companion object {
        fun fromFilename(filename: String): ProcessedFile {
            val extension = when {
                filename.endsWith(Extension.C.value) -> Extension.C
                filename.endsWith(Extension.IR.value) -> Extension.IR
                filename.endsWith(Extension.AR.value) -> Extension.AR
                filename.endsWith(Extension.ASM.value) -> Extension.ASM
                filename.endsWith(Extension.OBJ.value) -> Extension.OBJ
                filename.endsWith(Extension.SO.value) -> Extension.SO
                else -> Extension.EXE
            }

            return ProcessedFile(filename, extension)
        }

        fun create(filename: String, extension: Extension): ProcessedFile {
            return ProcessedFile(filename + extension.value, extension)
        }
    }
}