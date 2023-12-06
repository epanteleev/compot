package ir

import ir.types.Type

abstract class GlobalConstant(protected open val name: String): GlobalSymbol, Value {
    override fun name(): String {
        return name
    }

    override fun toString(): String {
        return "@$name"
    }

    abstract fun dump(): String
}

data class InitializedGlobalConstant(override val name: String, val data: Constant): GlobalConstant(name) {
    override fun type(): Type {
        return data.type().ptr()
    }

    override fun toString(): String {
        return super.toString()
    }

    override fun dump(): String {
        return "@$name = constant ${data.type()} $data"
    }
}

data class ZeroGlobalConstant(override val name: String, val type: Type): GlobalConstant(name) {
    override fun type(): Type {
        return type.ptr()
    }

    override fun toString(): String {
        return super.toString()
    }

    override fun dump(): String {
        return "@$name = constant $type"
    }
}