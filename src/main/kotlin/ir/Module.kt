package ir

data class ModuleException(override val message: String): Exception(message)

class Module(internal val functions: Map<Function, FunctionData>, internal val externFunctions: Set<ExternFunction>) {
    fun findFunction(function: Function): FunctionData {
        return functions[function] ?: throw ModuleException("Cannot find function: $function")
    }
}