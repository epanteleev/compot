package ir

data class ModuleException(override val message: String): Exception(message)

data class Module(internal val functions: List<FunctionData>, internal val externFunctions: Set<ExternFunction>) {
    val prototypes: List<AnyFunctionPrototype> by lazy {
        externFunctions.toList() + functions.map { it.prototype }
    }

    fun findFunction(prototype: FunctionPrototype): FunctionData {
        return functions.find { it.prototype == prototype }
            ?: throw ModuleException("Cannot find function: $prototype")
    }

    fun functions(): Iterator<FunctionData> {
        return functions.iterator()
    }
}