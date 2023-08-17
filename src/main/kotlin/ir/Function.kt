package ir

abstract class AnyFunction(val name: String, private val returnType: Type) {
    fun type(): Type {
        return returnType
    }
}

class Function(val index: Int, name: String, returnType: Type): AnyFunction(name, returnType)

class ExternFunction(name: String, returnType: Type, val arguments: List<Type>): AnyFunction(name, returnType) {
    override fun toString(): String {
        return "extern ${type()} $name(${arguments.joinToString()})"
    }
}