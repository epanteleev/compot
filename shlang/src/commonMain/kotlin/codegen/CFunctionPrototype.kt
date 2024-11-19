package codegen

import types.*
import ir.types.*
import typedesc.TypeHolder
import ir.attributes.ByValue
import codegen.TypeConverter.toIRType
import ir.attributes.FunctionAttribute
import ir.attributes.VarArgAttribute
import ir.module.builder.impl.ModuleBuilder
import tokenizer.Position


class CFunctionPrototype(val returnType: Type, val argumentTypes: List<NonTrivialType>, val attributes: Set<FunctionAttribute>)

internal class CFunctionPrototypeBuilder(val begin: Position, val functionType: AnyCFunctionType, val mb: ModuleBuilder, val typeHolder: TypeHolder) {
    private val returnType = irReturnType()
    private val types = arrayListOf<NonTrivialType>()
    private val attributes = hashSetOf<FunctionAttribute>()

    private fun irReturnType(): Type {
        val retType = functionType.retType()
        return when (val ty = retType.cType()) {
            is VOID -> Type.Void
            is BOOL -> Type.I8
            is CPrimitive -> mb.toIRType<PrimitiveType>(typeHolder, retType.cType())
            is CStructType -> {
                val list = CallConvention.coerceArgumentTypes(ty) ?: return Type.Void
                if (list.size == 1) {
                    list[0]
                } else {
                    TupleType(list.toTypedArray())
                }
            }
            else -> throw IRCodeGenError("Unknown return type, type=$retType", begin)
        }
    }

    private fun argumentTypes(): Pair<List<NonTrivialType>, MutableSet<FunctionAttribute>> { //TODO remove pair
        val cType = functionType.retType().cType()
        if (cType is AnyCStructType && !cType.isSmall()) {
            types.add(Type.Ptr)
        }
        for ((idx, type) in functionType.args().withIndex()) {
            when (val ty = type.cType()) {
                is AnyCStructType -> {
                    val parameters = CallConvention.coerceArgumentTypes(ty)
                    if (parameters != null) {
                        types.addAll(parameters)
                    } else {
                        types.add(mb.toIRType<StructType>(typeHolder, ty))
                        attributes.add(ByValue(idx))
                    }
                }
                is CArrayType, is CUncompletedArrayType -> types.add(Type.Ptr)
                is CPointer    -> types.add(Type.Ptr)
                is BOOL        -> types.add(Type.U8)
                is CPrimitive  -> types.add(mb.toIRType<PrimitiveType>(typeHolder, type.cType()))
                else -> throw IRCodeGenError("Unknown type, type=$type", begin) //FIXME argument positions!!!
            }
        }
        return Pair(types, attributes)
    }

    fun build(): CFunctionPrototype {
        argumentTypes()
        if (functionType.variadic()) {
            attributes.add(VarArgAttribute)
        }

        return CFunctionPrototype(returnType, types, attributes)
    }
}