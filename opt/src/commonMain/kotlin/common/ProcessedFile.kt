package common

enum class Extension(val value: String) {
    C(".c"),
    IR(".ir"),
    ASM(".s"),
    OBJ(".o"),
    EXE(".out")
}

class ProcessedFile private constructor(val filename: String, val extension: Extension) {
    fun basename(): String {
        return Files.getBasename(filename)
    }

    fun withExtension(extension: Extension): ProcessedFile {
        if (this.extension == extension) {
            return this
        }

        return create(filename, extension)
    }

    companion object {
        fun fromFilename(filename: String): ProcessedFile {
            val extension = when {
                filename.endsWith(Extension.C.value) -> Extension.C
                filename.endsWith(Extension.IR.value) -> Extension.IR
                filename.endsWith(Extension.ASM.value) -> Extension.ASM
                filename.endsWith(Extension.OBJ.value) -> Extension.OBJ
                else -> Extension.EXE
            }

            return ProcessedFile(filename, extension)
        }

        fun create(filename: String, extension: Extension): ProcessedFile {
            return ProcessedFile(filename + extension.value, extension)
        }
    }
}