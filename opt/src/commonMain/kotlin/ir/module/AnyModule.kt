package ir.module

abstract class Module<FD: AnyFunctionData>(internal val functions: Map<String, FD>) {
    abstract fun copy(): Module<FD>

    fun functions(): Collection<FD> {
        return functions.values
    }
}