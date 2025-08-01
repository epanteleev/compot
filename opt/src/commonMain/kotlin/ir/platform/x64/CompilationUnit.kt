package ir.platform.x64

import asm.x64.*
import ir.attributes.GlobalValueAttribute
import ir.types.*
import ir.global.*
import ir.value.constant.*
import ir.platform.common.CompiledModule
import ir.platform.x64.auxiliary.LinearizeInitializerList


// Using as
// The GNU Assembler
//
// https://ftp.gnu.org/old-gnu/Manuals/gas-2.9.1/html_node/as_toc.html
class CompilationUnit: CompiledModule, ObjModule(NameAssistant()) {
    fun mkConstant(globalConstant: GlobalConstant): ObjLabel = when (globalConstant) {
        is StringLiteralGlobalConstant -> {
            // name:
            //    .string "string"
            label(globalConstant.name()) {
                string(globalConstant.constant().toString())
                string("\"\"")
                size(globalConstant.name(), globalConstant.constant().length())
            }
        }
        is AggregateGlobalConstant -> makeAggregateConstant(globalConstant.name(), globalConstant.contentType().asType(), globalConstant.elements())
        is PrimitiveGlobalConstant -> makePrimitiveConstant(globalConstant)
    }

    fun makeGlobal(globalValue: AnyGlobalValue) {
        when (globalValue) {
            is GlobalValue -> {
                if (globalValue.attribute() != GlobalValueAttribute.INTERNAL) {
                    global(globalValue.name())
                }

                convertGlobalValueToSymbolType(globalValue)
            }
            is ExternValue -> {}
        }
    }

    private fun makeAggregateConstant(name: String, aggregateType: AggregateType, initializer: InitializerListValue): ObjLabel = label(name) {
        val linear = LinearizeInitializerList.linearize(initializer, aggregateType)
        for (e in linear) {
            when (e) {
                is PrimitiveConstant -> primitive(this, e)
                is StringLiteralConstant -> {
                    string(e.toString())
                    size(name, e.length())
                }
                else -> throw IllegalArgumentException("unsupported constant type: $e")
            }
        }
    }

    private fun makeStringLiteralConstant(globalValue: GlobalValue, type: ArrayType, constant: StringLiteralConstant): ObjLabel {
        require(globalValue.contentType() == type) {
            "type mismatch: ${globalValue.contentType()} != $type"
        }
        return label(globalValue.name()) {
            string(constant.toString())
            size(globalValue.name(), constant.length())
        }
    }

    private fun makePrimitiveConstant(globalValue: GlobalValue): ObjLabel = when (val initializer = globalValue.initializer()) {
        is InitializerListValue -> anonConstant {
            for (e in LinearizeInitializerList.linearize(initializer, globalValue.contentType().asType())) {
                when (e) {
                    is PrimitiveConstant -> primitive(this, e)
                    is StringLiteralConstant -> {
                        string(e.toString())
                        size(globalValue.name(), e.length())
                    }
                    else -> throw IllegalArgumentException("unsupported constant type: $e")
                }
            }
        }
        is StringLiteralConstant -> {
            val initConstant = anonConstant {
                string(initializer.toString())
                size(globalValue.name(), initializer.length())
            }
            label(globalValue.name()) {
                quad(initConstant)
            }
        }
        is PrimitiveConstant -> label(globalValue.name()) {
            primitive(this, initializer)
        }
    }

    private fun convertGlobalValueToSymbolType(globalValue: GlobalValue) {
        when (val type = globalValue.contentType()) {
            is StructType -> {
                val constant = globalValue.initializer() as InitializerListValue
                makeAggregateConstant(globalValue.name(), globalValue.contentType().asType(), constant)
            }
            is ArrayType -> when (val constant = globalValue.initializer()) {
                is InitializerListValue -> makeAggregateConstant(globalValue.name(), globalValue.contentType().asType(), constant)
                is StringLiteralConstant -> {
                    val arrayType = globalValue.contentType() as ArrayType
                    makeStringLiteralConstant(globalValue, type, constant)
                    if (arrayType.length > constant.content.length) {
                        zero(arrayType.length - constant.content.length)
                    }
                }
                else -> throw IllegalArgumentException("unsupported constant type: $constant")
            }
            is PrimitiveType -> makePrimitiveConstant(globalValue)
        }
    }

    private fun primitive(builder: ObjBuilder, data: PrimitiveConstant) = when (data) {
        is I64Value -> builder.quad(data.i64)
        is U64Value -> builder.quad(data.u64)
        is I32Value -> builder.long(data.i32)
        is U32Value -> builder.long(data.u32)
        is I16Value -> builder.short(data.i16)
        is U16Value -> builder.short(data.u16.toShort())
        is I8Value  -> builder.byte(data.i8)
        is U8Value  -> builder.byte(data.u8.toByte())
        is F32Value -> builder.long(data.bits())
        is F64Value -> builder.quad(data.bits())
        is PointerLiteral -> {
            val gConstant = data.gConstant
            builder.quad(findLabel(gConstant.name()), data.index)
        }
        is NullValue -> builder.quad(0)
        UndefValue -> TODO()
    }

    private fun makePrimitiveConstant(globalValue: PrimitiveGlobalConstant): ObjLabel {
        return label(globalValue.name()) {
            primitive(this, globalValue.constant())
        }
    }
}