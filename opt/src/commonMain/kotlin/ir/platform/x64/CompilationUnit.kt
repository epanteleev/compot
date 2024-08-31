package ir.platform.x64

import asm.x64.*
import ir.value.*
import ir.global.*
import ir.platform.common.CompiledModule
import ir.platform.x64.codegen.MacroAssembler
import ir.types.*

// Using as
// The GNU Assembler
//
// https://ftp.gnu.org/old-gnu/Manuals/gas-2.9.1/html_node/as_toc.html
class CompilationUnit: CompiledModule() {
    private val symbols = arrayListOf<AnyDirective>()
    private val namedDirectives = hashSetOf<NamedDirective>()
    private var nameCounter = 0

    private fun newName() = ".loc.constant.${nameCounter++}"

    fun section(section: SectionDirective) {
        symbols.add(section)
    }

    fun global(name: String) {
        addSymbol(GlobalDirective(name))
    }

    private inline fun<reified T: AnyDirective> addSymbol(objSymbol: T) {
        if (objSymbol is NamedDirective) {
            val has = namedDirectives.add(objSymbol)
            if (!has) {
                throw IllegalArgumentException("symbol already exists: $objSymbol")
            }
        }
        symbols.add(objSymbol)
    }

    fun mkFunction(name: String): MacroAssembler {
        val fn = MacroAssembler(name)
        val obj = ObjLabel(name)
        symbols.add(obj)
        obj.anonymousDirective.add(fn)
        return fn
    }

    private fun makeAggregateConstant(globalValue: AnyAggregateGlobalConstant): Directive {
        return makeAggregateConstant(globalValue.name(), globalValue.elements())
    }

    private fun makeAggregateConstant(name: String, initializer: InitializerListValue): Directive {
        val types = arrayListOf<SymbolType>()
        val data = arrayListOf<String>()
        for (e in initializer.linearize()) {
            types.addAll(convertToSymbolType(e))
            data.add(e.data())
        }

        return Directive(name, data, types)
    }

    fun mkConstant(globalValue: GlobalConstant) = when (globalValue) {
        is StringLiteralGlobalConstant -> {
            addSymbol(StringSymbol(globalValue.name(), globalValue.data()))
        }
        is AggregateGlobalConstant -> addSymbol(makeAggregateConstant(globalValue))
        else -> addSymbol(Directive(globalValue.name(), listOf(globalValue.data()), convertToSymbolType(globalValue.constant())))
    }

    fun makeGlobal(globalValue: AnyGlobalValue) {
        val symbol = convertGlobalValueToSymbolType(globalValue)
        if (symbol != null) {
            addSymbol(symbol)
        }
    }

    private fun convertGlobalValueToSymbolType(globalValue: AnyGlobalValue): AnyDirective? {
        if (globalValue is ExternValue) {
            return null
        }
        globalValue as GlobalValue

        when (val type = globalValue.contentType()) {
            is StructType -> {
                val constant = globalValue.initializer() as InitializerListValue
                return makeAggregateConstant(globalValue.name(), constant)
            }
            is ArrayType -> {
                when (val constant = globalValue.initializer()) {
                    is InitializerListValue -> return makeAggregateConstant(globalValue.name(), constant)
                    is StringLiteralConstant -> {
                        val initializerName = newName()
                        val initializer = StringSymbol(initializerName, constant.data())
                        addSymbol(initializer)
                        val init = StringBuilder()
                        var i = 0
                        for (c in constant.linearize()) {
                            init.append(c)
                            i += 1
                        }

                        for (j in i until type.length) {
                            init.append("\\000")
                        }

                        return AsciiSymbol(globalValue.name(), init.toString())
                    }
                    else -> throw IllegalArgumentException("unsupported constant type: $constant")
                }
            }
            else -> {
                val initializer = globalValue.initializer()
                val symbolType = convertToSymbolType(initializer)
                return when (initializer) {
                    is InitializerListValue -> {
                        val initConstant = makeAggregateConstant(newName(), initializer)
                        addSymbol(initConstant)
                        Directive(globalValue.name(), listOf(initConstant.name), symbolType)
                    }
                    is StringLiteralConstant -> {
                        val initConstant = StringSymbol(newName(), initializer.data())
                        addSymbol(initConstant)
                        QuadSymbol(globalValue.name(), initConstant.name)
                    }
                    else -> Directive(globalValue.name(), listOf(globalValue.data()), symbolType)
                }
            }
        }
    }

    private fun makePrimitiveConstant(globalValue: GlobalConstant): Directive {
        val symbolType = convertToSymbolType(globalValue.constant())
        return Directive(globalValue.name(), listOf(globalValue.data()), symbolType)
    }

    private fun convertToSymbolType(globalValue: Constant): List<SymbolType> {
        val sym = when (globalValue.type()) {
            Type.I64 -> SymbolType.Quad
            Type.U64 -> SymbolType.Quad
            Type.I32 -> SymbolType.Long
            Type.U32 -> SymbolType.Long
            Type.I16 -> SymbolType.Short
            Type.U16 -> SymbolType.Short
            Type.I8  -> SymbolType.Byte
            Type.U8  -> SymbolType.Byte
            Type.F32 -> SymbolType.Long
            Type.F64 -> SymbolType.Quad
            Type.Ptr -> SymbolType.Quad
            else -> null
        }
        if (sym != null) {
            return listOf(sym)
        }

        when (globalValue) {
            is StringLiteralConstant -> return listOf(SymbolType.StringLiteral)
            is InitializerListValue -> {
                val types = arrayListOf<SymbolType>()
                for (e in globalValue.linearize()) {
                    types.addAll(convertToSymbolType(e))
                }
                return types
            }
            else -> throw IllegalArgumentException("unsupported constant type: $globalValue")
        }
    }

    override fun toString(): String {
        val builder = StringBuilder()
        for ((idx, symbol) in symbols.withIndex()) {
            builder.append(symbol)
            if (idx < symbols.size - 1) {
                builder.append("\n")
            }
        }
        builder.append("\n")
        return builder.toString()
    }
}