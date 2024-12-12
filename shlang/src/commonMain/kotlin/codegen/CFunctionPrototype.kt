package codegen

import types.*
import ir.types.*
import typedesc.TypeHolder
import ir.attributes.ByValue
import codegen.TypeConverter.toIRType
import intrinsic.VaStart
import ir.attributes.FunctionAttribute
import ir.attributes.GlobalValueAttribute
import ir.attributes.VarArgAttribute
import ir.module.builder.impl.ModuleBuilder
import tokenizer.Position
import typedesc.StorageClass


class CFunctionPrototype(val returnType: Type, val argumentTypes: List<NonTrivialType>, val attributes: Set<FunctionAttribute>)

internal class CFunctionPrototypeBuilder(val begin: Position, private val functionType: AnyCFunctionType, private val mb: ModuleBuilder, val typeHolder: TypeHolder, val storageClass: StorageClass?) {
    private val returnType = irReturnType()
    private val types = arrayListOf<NonTrivialType>()
    private val attributes = hashSetOf<FunctionAttribute>()

    private fun handleStorageClass() {
        val attribute = when (storageClass) {
            StorageClass.STATIC -> GlobalValueAttribute.INTERNAL
            else -> GlobalValueAttribute.DEFAULT
        }

        attributes.add(attribute)
    }

    private fun irReturnType(): Type {
        val retType = functionType.retType()
        return when (val ty = retType.cType()) {
            is VOID -> VoidType
            is BOOL -> I8Type
            is CPrimitive -> mb.toIRType<PrimitiveType>(typeHolder, retType.cType())
            is CStructType -> {
                val list = CallConvention.coerceArgumentTypes(ty) ?: return VoidType
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
            types.add(PtrType)
        }
        for ((idx, type) in functionType.args().withIndex()) {
            when (val ty = type.cType()) {
                is AnyCStructType -> {
                    if (ty === VaStart.vaList) {
                        types.add(PtrType)
                        continue
                    }
                    if (!ty.isSmall()) {
                        types.add(mb.toIRType<StructType>(typeHolder, ty))
                        attributes.add(ByValue(idx))
                        continue
                    }
                    val parameters = CallConvention.coerceArgumentTypes(ty) ?: throw RuntimeException("Unsupported type, type=$ty")
                    types.addAll(parameters)
                }
                is CArrayType, is CUncompletedArrayType -> types.add(PtrType)
                is CPointer    -> types.add(PtrType)
                is BOOL        -> types.add(U8Type)
                is CPrimitive  -> types.add(mb.toIRType<PrimitiveType>(typeHolder, type.cType()))
                else -> throw IRCodeGenError("Unknown type, type=$type", begin) //FIXME argument positions!!!
            }
        }
        return Pair(types, attributes)
    }

    fun build(): CFunctionPrototype {
        argumentTypes()
        handleStorageClass()
        if (functionType.variadic()) {
            attributes.add(VarArgAttribute)
        }

        return CFunctionPrototype(returnType, types, attributes)
    }
}