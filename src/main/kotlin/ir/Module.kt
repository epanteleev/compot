package ir

data class ModuleException(override val message: String): Exception(message)

class Module(internal val functions: Map<Function, FunctionData>) {
    fun findFunction(function: Function): FunctionData {
        return functions[function] ?: throw ModuleException("Cannot find function: $function")
    }
    
    companion object {
        fun create(functions: Map<Function, FunctionData>): Module {
            return Module(functions)
        }
    }
}