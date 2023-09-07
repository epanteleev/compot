package ir

data class ModuleException(override val message: String): Exception(message)

class Module(internal val functions: List<FunctionData>, internal val externFunctions: Set<ExternFunction>) {
    fun findFunction(prototype: FunctionPrototype): FunctionData {
        return functions.find { it.prototype == prototype }
            ?: throw ModuleException("Cannot find function: $prototype")
    }

    fun functions(): Iterator<FunctionData> {
        return functions.iterator()
    }
}