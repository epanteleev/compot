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


internal class CFunctionPrototype(val returnType: Type, val argumentTypes: List<NonTrivialType>, val attributes: Set<FunctionAttribute>)

internal class CFunctionPrototypeBuilder(
    private val begin: Position,
    private val functionType: AnyCFunctionType,
    private val mb: ModuleBuilder,
    val typeHolder: TypeHolder,
    val storageClass: StorageClass?,
) {
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
            is AnyCStructType -> {
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

    private fun argumentTypes() {
        val cType = functionType.retType().cType()
        var offset = 0
        if (cType is AnyCStructType && !cType.isSmall()) {
            offset += 1
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
                        val irType = mb.toIRType<StructType>(typeHolder, ty)
                        types.add(irType)
                        attributes.add(ByValue(idx + offset, irType))
                        continue
                    }
                    val parameters = CallConvention.coerceArgumentTypes(ty) ?: throw RuntimeException("Unsupported type, type=$ty")
                    types.addAll(parameters)
                    offset += parameters.size - 1
                }
                is CArrayType, is CUncompletedArrayType -> types.add(PtrType)
                is CPointer    -> types.add(PtrType)
                is BOOL        -> types.add(I8Type)
                is CPrimitive  -> types.add(mb.toIRType<PrimitiveType>(typeHolder, type.cType()))
                else -> throw IRCodeGenError("Unknown type, type=$type", begin)
            }
        }
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