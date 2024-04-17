package ir.platform.x64

import ir.*
import asm.x64.*
import ir.platform.common.CompiledModule


class CompilationUnit: CompiledModule() {
    private val functions = arrayListOf<Assembler>()
    private val symbols = mutableSetOf<ObjSymbol>()

    fun mkFunction(name: String): Assembler {
        val fn = Assembler(name)
        functions.add(fn)
        return fn
    }

    fun mkSymbol(globalValue: GlobalValue) {
        symbols.add(ObjSymbol(globalValue.name(), globalValue.data(), convertToSymbolType(globalValue)))
    }

    private fun convertToSymbolType(globalValue: GlobalValue): SymbolType {
        return when (globalValue) {
            is StringLiteralGlobal -> SymbolType.StringLiteral
            is I64GlobalValue      -> SymbolType.Quad
            is U64GlobalValue      -> SymbolType.Quad
            is I32GlobalValue      -> SymbolType.Long
            is U32GlobalValue      -> SymbolType.Long
            is I16GlobalValue      -> SymbolType.Short
            is U16GlobalValue      -> SymbolType.Short
            is I8GlobalValue       -> SymbolType.Byte
            is U8GlobalValue       -> SymbolType.Byte
            is F32GlobalValue      -> SymbolType.Long
            is F64GlobalValue      -> SymbolType.Quad
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