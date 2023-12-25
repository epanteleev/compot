package asm.x64

import ir.*

class Assembler {
    private val functions = arrayListOf<ObjFunction>()
    private val symbols = mutableSetOf<ObjSymbol>()

    fun mkFunction(name: String): ObjFunction {
        val fn = ObjFunction(name)
        functions.add(fn)
        return fn
    }

    fun mkSymbol(globalValue: GlobalValue) {
        symbols.add(ObjSymbol(globalValue.name(), globalValue.data(), convertToSymbolType(globalValue)))
    }

    private fun convertToSymbolType(globalValue: GlobalValue): SymbolType {
        return when (globalValue) {
            is StringLiteralGlobal -> SymbolType.StringLiteral
            is I64GlobalValue      -> SymbolType.Long
            is U64GlobalValue      -> SymbolType.Long
            is I32GlobalValue      -> SymbolType.Integer
            is U32GlobalValue      -> SymbolType.Integer
            is I16GlobalValue      -> SymbolType.Short
            is U16GlobalValue      -> SymbolType.Short
            is I8GlobalValue       -> SymbolType.Byte
            is U8GlobalValue       -> SymbolType.Byte
            else -> throw RuntimeException("unknown globals value: globalvalue=$globalValue")
        }
    }

    override fun toString(): String {
        val builder = StringBuilder()
        functions.forEach {
            builder.append(".global ${it.name()}\n")
        }
        builder.append('\n')

        if (symbols.isNotEmpty()) {
            builder.append(".data\n")
        }
        symbols.forEach {
            builder.append(it)
            builder.append('\n')
        }

        builder.append(".text\n")
        functions.joinTo(builder, separator = "\n")
        return builder.toString()
    }
}