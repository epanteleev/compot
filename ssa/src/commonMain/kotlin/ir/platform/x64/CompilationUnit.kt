package ir.platform.x64

import asm.x64.*
import ir.global.*
import ir.platform.common.CompiledModule
import ir.platform.x64.codegen.MacroAssembler


class CompilationUnit: CompiledModule() {
    private val functions = arrayListOf<MacroAssembler>()
    private val symbols = mutableSetOf<ObjSymbol>()

    fun mkFunction(name: String): MacroAssembler {
        val fn = MacroAssembler(name)
        functions.add(fn)
        return fn
    }

    fun mkConstant(globalValue: GlobalConstant) {
        symbols.add(ObjSymbol(globalValue.name(), globalValue.data(), convertToSymbolType(globalValue)))
    }

    fun makeGlobal(globalValue: GlobalValue) {
        symbols.add(convertGlobalValueToSymbolType(globalValue))
    }

    private fun convertGlobalValueToSymbolType(globalValue: GlobalValue): ObjSymbol {
        val symbolType = convertToSymbolType(globalValue.data)
        return if (symbolType == SymbolType.StringLiteral) {
            ObjSymbol(globalValue.name(), globalValue.data.name(), SymbolType.Quad)
        } else {
            ObjSymbol(globalValue.name(), globalValue.data(), symbolType)
        }
    }

    private fun convertToSymbolType(globalValue: GlobalConstant): SymbolType {
        return when (globalValue) {
            is StringLiteralConstant -> SymbolType.StringLiteral
            is I64ConstantValue -> SymbolType.Quad
            is U64ConstantValue -> SymbolType.Quad
            is I32ConstantValue -> SymbolType.Long
            is U32ConstantValue -> SymbolType.Long
            is I16ConstantValue -> SymbolType.Short
            is U16ConstantValue -> SymbolType.Short
            is I8ConstantValue  -> SymbolType.Byte
            is U8ConstantValue  -> SymbolType.Byte
            is F32ConstantValue -> SymbolType.Long
            is F64ConstantValue -> SymbolType.Quad
            else -> throw RuntimeException("unknown globals value: globalvalue=$globalValue:${globalValue.type()}")
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