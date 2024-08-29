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
    private val functions = arrayListOf<MacroAssembler>()
    private val symbols = hashMapOf<String, AnyObjSymbol>()
    private var nameCounter = 0

    private fun newName() = ".loc.constant.${nameCounter++}"

    private fun addSymbol(objSymbol: AnyObjSymbol) {
        val has = symbols.put(objSymbol.name(), objSymbol)
        if (has != null) {
            throw IllegalArgumentException("symbol with name='${objSymbol.name()}' already exists: old='$has', new='$objSymbol'")
        }
    }

    fun mkFunction(name: String): MacroAssembler {
        val fn = MacroAssembler(name)
        functions.add(fn)
        return fn
    }

    private fun makeAggregateConstant(globalValue: AnyAggregateGlobalConstant): ObjSymbol {
        return makeAggregateConstant(globalValue.name(), globalValue.elements())
    }

    private fun makeAggregateConstant(name: String, initializer: InitializerListValue): ObjSymbol {
        val types = arrayListOf<SymbolType>()
        val data = arrayListOf<String>()
        for (e in initializer.linearize()) {
            types.addAll(convertToSymbolType(e))
            data.add(e.data())
        }

        return ObjSymbol(name, data, types)
    }

    fun mkConstant(globalValue: GlobalConstant) = when (globalValue) {
        is StringLiteralGlobalConstant -> {
            addSymbol(ObjSymbol(globalValue.name(), listOf(globalValue.data()), listOf(SymbolType.StringLiteral)))
        }
        is AggregateGlobalConstant -> addSymbol(makeAggregateConstant(globalValue))
        else -> addSymbol(ObjSymbol(globalValue.name(), listOf(globalValue.data()), convertToSymbolType(globalValue.constant())))
    }

    fun makeGlobal(globalValue: AnyGlobalValue) {
        val symbol = convertGlobalValueToSymbolType(globalValue)
        if (symbol != null) {
            addSymbol(symbol)
        }
    }

    private fun convertGlobalValueToSymbolType(globalValue: AnyGlobalValue): AnyObjSymbol? {
        if (globalValue is ExternValue) {
            return null
        }
        globalValue as GlobalValue

        when (val type = globalValue.contentType()) {
            is StructType -> {
                val constant = globalValue.initializer() as InitializerListValue
                val symbolType = convertToSymbolType(constant)
                val data = constant.linearize().map { it.data() }

                return ObjSymbol(globalValue.name(), data, symbolType)
            }
            is ArrayType -> {
                when (val constant = globalValue.initializer()) {
                    is InitializerListValue -> {
                        val symbolType = convertToSymbolType(type)
                        val data = constant.linearize().map { it.data() }
                        return ObjSymbol(globalValue.name(), data, symbolType)
                    }
                    is StringLiteralConstant -> {
                        val initializerName = newName()
                        val initializer = ObjSymbol(initializerName, listOf(constant.data()), listOf(SymbolType.StringLiteral))
                        addSymbol(initializer)
                        val init = arrayListOf<String>()
                        var i = 0
                        for (c in constant.data()) {
                            init.add(initializerName)
                            i += 1
                        }

                        for (j in i until type.length) {
                            init.add("\\000")
                        }

                        return ObjSymbol(globalValue.name(), listOf(initializerName), listOf(SymbolType.Asciiz))
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
                        ObjSymbol(globalValue.name(), listOf(initConstant.name()), symbolType)
                    }
                    is StringLiteralConstant -> {
                        val initConstant = ObjSymbol(newName(), listOf(initializer.data()), listOf(SymbolType.StringLiteral))
                        addSymbol(initConstant)
                        ObjSymbol(globalValue.name(), listOf(initConstant.name()), listOf(SymbolType.Quad))
                    }
                    else -> ObjSymbol(globalValue.name(), listOf(globalValue.data()), symbolType)
                }
            }
        }
    }

    private fun convertToSymbolType(type: NonTrivialType): List<SymbolType> {
        val t = when (type) {
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

        if (t != null) {
            return listOf(t)
        }

        when (type) {
            is AggregateType -> {
                val types = arrayListOf<SymbolType>()
                for (f in type.fields()) {
                    types.addAll(convertToSymbolType(f))
                }
                return types
            }
            else -> throw IllegalArgumentException("unsupported type: $type")
        }
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
        functions.forEach {
            builder.append(".global ${it.name()}\n")
        }
        builder.append('\n')

        if (symbols.isNotEmpty()) {
            builder.append(".data\n")
        }
        symbols.values.forEach {
            builder.append(it)
            builder.append('\n')
        }

        builder.append(".text\n")
        functions.joinTo(builder, separator = "\n")
        return builder.toString()
    }
}