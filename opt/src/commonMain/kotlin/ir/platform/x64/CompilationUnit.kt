package ir.platform.x64

import asm.x64.*
import ir.value.*
import ir.global.*
import ir.platform.common.CompiledModule
import ir.types.*

// Using as
// The GNU Assembler
//
// https://ftp.gnu.org/old-gnu/Manuals/gas-2.9.1/html_node/as_toc.html
class CompilationUnit: CompiledModule, ObjModule(NameAssistant()) {
    private fun makeAggregateConstant(name: String, initializer: InitializerListValue): ObjLabel = label(name) {
        for (e in initializer.linearize()) {
            primitive(this, e.asType(), e.data())
        }
    }

    fun mkConstant(globalValue: GlobalConstant): ObjLabel = when (globalValue) {
        is StringLiteralGlobalConstant -> {
            // name:
            //    .string "string"
            label(globalValue.name()) {
                string(globalValue.data())
            }
        }
        is AggregateGlobalConstant -> makeAggregateConstant(globalValue.name(), globalValue.elements())
        is PrimitiveGlobalConstant -> makePrimitiveConstant(globalValue)
    }

    fun makeGlobal(globalValue: AnyGlobalValue) {
        if (globalValue is ExternValue) {
            return
        }
        convertGlobalValueToSymbolType(globalValue as GlobalValue)
    }

    private fun makeStringLiteralConstant(globalValue: GlobalValue, type: ArrayType, constant: StringLiteralConstant): ObjLabel {
        val values = constant.linearize()
        if (values.size == type.length) {
            return label(globalValue.name()) {
                string(constant.data())
            }
        }
        if (values.isNotEmpty()) {
            throw IllegalArgumentException("string too long: $values")
        }
        val init = StringBuilder()

        for (j in 0 until type.length) {
            init.append("\\000")
        }

        return label(globalValue.name()) {
            ascii(init.toString())
        }
    }

    private fun makePrimitiveConstant(globalValue: GlobalValue): ObjLabel = when (val initializer = globalValue.initializer()) {
        is InitializerListValue -> anonConstant {
            for (e in initializer.linearize()) {
                primitive(this, e.asType(), e.data())
            }
        }
        is StringLiteralConstant -> {
            val initConstant = anonConstant {
                string(initializer.data())
            }
            label(globalValue.name()) {
                quad(initConstant.name)
            }
        }
        is PrimitiveConstant -> label(globalValue.name()) {
            primitive(this, globalValue.asType(), initializer.data())
        }
        else -> throw IllegalArgumentException("unsupported constant type: $initializer")
    }

    private fun convertGlobalValueToSymbolType(globalValue: GlobalValue) = when (val type = globalValue.contentType()) {
        is StructType -> {
            val constant = globalValue.initializer() as InitializerListValue
            makeAggregateConstant(globalValue.name(), constant)
        }
        is ArrayType -> when (val constant = globalValue.initializer()) {
            is InitializerListValue  -> makeAggregateConstant(globalValue.name(), constant)
            is StringLiteralConstant -> makeStringLiteralConstant(globalValue, type, constant)
            else -> throw IllegalArgumentException("unsupported constant type: $constant")
        }
        is PrimitiveType -> makePrimitiveConstant(globalValue)
    }

    private fun primitive(builder: ObjBuilder, type: PrimitiveType, data: String) = when (type) {
        Type.I64 -> builder.quad(data)
        Type.U64 -> builder.quad(data)
        Type.I32 -> builder.long(data)
        Type.U32 -> builder.long(data)
        Type.I16 -> builder.short(data)
        Type.U16 -> builder.short(data)
        Type.I8  -> builder.byte(data)
        Type.U8  -> builder.byte(data)
        Type.F32 -> builder.long(data)
        Type.F64 -> builder.quad(data)
        Type.Ptr -> builder.quad(data)
        else -> throw IllegalArgumentException("unsupported constant type: $type")
    }

    private fun makePrimitiveConstant(globalValue: GlobalConstant): ObjLabel {
        val data = globalValue.data()
        return label(globalValue.name()) {
            primitive(this, globalValue.asType(), data)
        }
    }
}