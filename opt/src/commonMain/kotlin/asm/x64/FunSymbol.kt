package asm.x64

sealed class FunSymbol(val name: String)

class InternalFunSymbol(name: String): FunSymbol(name) {
    override fun toString(): String = name
}

class ExternalFunSymbol(name: String): FunSymbol(name) {
    override fun toString(): String = "$name@PLT"
}