package gen.consteval

interface ConstEvalContext {
    fun getVariable(name: String): Int
    fun callFunction(name: String, args: List<Int>): Int
}