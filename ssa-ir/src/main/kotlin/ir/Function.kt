package ir

abstract class AnyFunction(val name: String, protected val returnType: Type) {
    fun type(): Type {
        return returnType
    }
}

class FunctionPrototype(name: String, returnType: Type, private val arguments: List<Type>): AnyFunction(name, returnType) {
    override fun toString(): String {
        val builder = StringBuilder()
        builder.append("define $name(")
        arguments.joinTo(builder)
        builder.append("): $returnType")
        return builder.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FunctionPrototype

        return arguments == other.arguments
    }

    override fun hashCode(): Int {
        return name.hashCode() + arguments.hashCode() + returnType.hashCode()
    }
}

class ExternFunction(name: String, returnType: Type, private val arguments: List<Type>): AnyFunction(name, returnType) {
    override fun toString(): String {
        return "extern ${type()} $name(${arguments.joinToString()})"
    }
}