package sema

import codegen.consteval.ArraySizeConstEvalContext
import codegen.consteval.ConstEvalExpression
import codegen.consteval.TryConstEvalExpressionLong
import common.assertion
import parser.nodes.AbstractDeclarator
import parser.nodes.AnyDeclarator
import parser.nodes.ArrayDeclarator
import parser.nodes.Declarator
import parser.nodes.DirectDeclarator
import parser.nodes.DirectVarDeclarator
import parser.nodes.EmptyExpression
import parser.nodes.ExpressionInitializer
import parser.nodes.FunctionDeclarator
import parser.nodes.IdentifierList
import parser.nodes.InitDeclarator
import parser.nodes.InitializerListInitializer
import parser.nodes.NodePointer
import parser.nodes.Parameter
import parser.nodes.ParameterTypeList
import parser.nodes.ParameterVarArg
import parser.nodes.StringNode
import typedesc.DeclSpec
import typedesc.StorageClass
import typedesc.TypeDesc
import typedesc.TypeHolder
import typedesc.TypeResolutionException
import typedesc.Typedef
import typedesc.VarDescriptor
import types.AbstractCFunction
import types.CArrayType
import types.CFunctionType
import types.CPointer
import types.CStringLiteral
import types.CType
import types.CUncompletedArrayType
import types.InitializerType
import types.VOID
import types.asType

class SemanticAnalysis(val typeHolder: TypeHolder) {
    private fun wrapPointers(type: CType, pointers: List<NodePointer>): CType {
        var pointerType = type
        for (pointer in pointers) {
            pointerType = CPointer(pointerType, pointer.property())
        }
        return pointerType
    }

    fun resolveTypedef(declarator: Declarator, declSpec: DeclSpec): Typedef? {
        if (declSpec.storageClass != StorageClass.TYPEDEF) {
            return null
        }

        val pointerType = wrapPointers(declSpec.typeDesc.cType(), declarator.pointers)
        val newTypeDesc = TypeDesc.from(pointerType, declSpec.typeDesc.qualifiers())

        val type = resolveDirectDeclarator(declarator.directDeclarator, newTypeDesc)
        return Typedef(declarator.name(), type)
    }

    fun declareVar(declarator: AnyDeclarator, declSpec: DeclSpec): VarDescriptor? = when (declarator) {
        is Declarator -> declareVar(declarator, declSpec)
        is InitDeclarator -> declareVar(declarator, declSpec)
    }

    private fun declareVar(declarator: Declarator, declSpec: DeclSpec): VarDescriptor? {
        if (declSpec.storageClass == StorageClass.TYPEDEF) {
            return null
        }

        val pointerType = wrapPointers(declSpec.typeDesc.cType(), declarator.pointers)
        val newTypeDesc = TypeDesc.from(pointerType, declSpec.typeDesc.qualifiers())
        val type = resolveDirectDeclarator(declarator.directDeclarator, newTypeDesc)
        return VarDescriptor(declarator.name(), type.asType(declarator.begin()), type.qualifiers(), declSpec.storageClass)
    }

    private fun declareVar(initDeclarator: InitDeclarator, declSpec: DeclSpec): VarDescriptor {
        val pointerType = wrapPointers(declSpec.typeDesc.cType(), initDeclarator.declarator.pointers)
        val newTypeDesc = TypeDesc.from(pointerType, declSpec.typeDesc.qualifiers())

        val type = resolveDirectDeclarator(initDeclarator.declarator.directDeclarator, newTypeDesc)
        val baseType = type.cType()
        if (baseType !is CUncompletedArrayType) {
            return VarDescriptor(initDeclarator.name(), baseType.asType(initDeclarator.begin()), type.qualifiers(), declSpec.storageClass)
        }

        when (initDeclarator.rvalue) {
            is InitializerListInitializer -> {
                // Special case for array initialization without exact size like:
                // int a[] = {1, 2};
                // 'a' is array of 2 elements, not pointer to int

                val initializerList = initDeclarator.rvalue.list
                when (val initializerType = initializerList.resolveType(typeHolder)) {
                    is InitializerType -> {
                        val rvalueType = CArrayType(baseType.element(), initializerList.length().toLong())
                        return VarDescriptor(initDeclarator.name(), rvalueType, listOf(), declSpec.storageClass)
                    }
                    is CStringLiteral -> {
                        val rvalueType = CArrayType(baseType.element(), initializerType.dimension + 1)
                        return VarDescriptor(initDeclarator.name(), rvalueType, listOf(), declSpec.storageClass)
                    }
                    else -> throw TypeResolutionException("Array size is not specified: type=$initializerType", initDeclarator.declarator.begin())
                }
            }
            is ExpressionInitializer -> {
                val expr = initDeclarator.rvalue.expr
                if (expr !is StringNode) {
                    throw TypeResolutionException("Array size is not specified", initDeclarator.declarator.begin())
                }
                // Special case for string initialization like:
                // char a[] = "hello";
                return VarDescriptor(initDeclarator.name(), expr.resolveType(typeHolder), listOf(), declSpec.storageClass)
            }
        }
    }


    private fun resolveAllDeclaratorsInDirectDeclarator(directDeclarator: DirectDeclarator, baseType: TypeDesc): TypeDesc {
        var currentType = baseType
        for (directDeclaratorParam in directDeclarator.directDeclaratorParams.reversed()) {
            when (directDeclaratorParam) {
                is ArrayDeclarator -> {
                    currentType = resolveArrayDeclaratorType(directDeclaratorParam, currentType)
                }

                is ParameterTypeList -> {
                    val abstractType = resolveParameterTypeList(directDeclaratorParam, currentType)
                    currentType = TypeDesc.from(CFunctionType(directDeclarator.name(), abstractType.cType() as AbstractCFunction), abstractType.qualifiers())
                }

                is IdentifierList -> throw IllegalStateException("Identifier list is not supported")
            }
        }
        return currentType
    }

    private fun resolveDirectDeclarator(directDeclarator: DirectDeclarator, baseType: TypeDesc): TypeDesc = when (directDeclarator.decl) {
        is FunctionDeclarator -> {
            assertion(directDeclarator.directDeclaratorParams.size == 1) { "Function pointer should have only one parameter" }
            val pointers = directDeclarator.decl.declarator.pointers
            if (pointers.isEmpty()) {
                resolveAllDeclaratorsInDirectDeclarator(directDeclarator, baseType)
            } else {
                val type = resolveParameterTypeList(directDeclarator.parameterTypeList(), baseType)
                resolveFunctionDeclarator(directDeclarator.decl, type)
            }
        }
        is DirectVarDeclarator -> resolveAllDeclaratorsInDirectDeclarator(directDeclarator, baseType)
    }

    private fun resolveFunctionDeclarator(functionDeclarator: FunctionDeclarator, typeDesc: TypeDesc): TypeDesc {
        val cType = if (typeDesc.cType() is AbstractCFunction) {
            TypeDesc.from(CPointer(typeDesc.cType() as AbstractCFunction, setOf()), listOf())
        } else {
            typeDesc
        }

        return resolveDirectDeclarator(functionDeclarator.declarator.directDeclarator, cType)
    }

    fun resolveParameterList(parameterTypeList: ParameterTypeList): List<VarDescriptor>? {
        if (parameterTypeList.params.isEmpty()) {
            return emptyList()
        }
        val varDescs = arrayListOf<VarDescriptor>()
        for (param in parameterTypeList.params) {
            if (param !is Parameter) {
                continue
            }

            val varDesc = resolveParameterVarDesc(param) ?: return null
            varDescs.add(varDesc)
        }

        return varDescs
    }

    fun resolveParameterVarDesc(parameter: Parameter): VarDescriptor? {
        val type = resolveParameterType( parameter)
        val name = parameter.name() ?: return null
        return VarDescriptor(name, type.asType(parameter.begin()), type.qualifiers(), null)
    }

    fun resolveParameterType(parameter: Parameter): TypeDesc {
        val type = parameter.declspec.specifyType(typeHolder)
        return parameter.paramDeclarator.resolveType(type, typeHolder)
    }

    fun resolveAbstractDeclaratorType(abstractDeclarator: AbstractDeclarator, typeDesc: TypeDesc): TypeDesc {
        var pointerType = typeDesc.cType()
        for (pointer in abstractDeclarator.pointers) { //TODO wrap pointers fn
            pointerType = CPointer(pointerType, pointer.property())
        }
        var newTypeDesc = TypeDesc.from(pointerType)
        if (abstractDeclarator.directAbstractDeclarators == null) {
            return newTypeDesc
        }

        for (abstractDeclarator in abstractDeclarator.directAbstractDeclarators.reversed()) {
            newTypeDesc = when (abstractDeclarator) {
                is ArrayDeclarator -> resolveArrayDeclaratorType(abstractDeclarator, newTypeDesc)
                is ParameterTypeList -> resolveParameterTypeList(abstractDeclarator, newTypeDesc)
                is AbstractDeclarator -> resolveAbstractDeclaratorType(abstractDeclarator, newTypeDesc)
            }
        }

        return newTypeDesc
    }

    private fun resolveArrayDeclaratorType(arrayDeclarator: ArrayDeclarator, typeDesc: TypeDesc): TypeDesc {
        if (arrayDeclarator.constexpr is EmptyExpression) {
            return TypeDesc.from(CUncompletedArrayType(typeDesc))
        }

        val ctx = ArraySizeConstEvalContext(typeHolder)
        val size = ConstEvalExpression.eval(arrayDeclarator.constexpr, TryConstEvalExpressionLong(ctx))
            ?: return TypeDesc.from(CUncompletedArrayType(typeDesc))
        return TypeDesc.from(CArrayType(typeDesc, size))
    }

    private fun resolveParameterTypeList(parameterTypeList: ParameterTypeList, typeDesc: TypeDesc): TypeDesc {
        val params = resolveParamTypeListTypes(parameterTypeList)
        return TypeDesc.from(AbstractCFunction(typeDesc, params, parameterTypeList.isVarArg()), arrayListOf())
    }

    private fun resolveParamTypeListTypes(parameterTypeList: ParameterTypeList): List<TypeDesc> {
        if (parameterTypeList.params.size == 1) {
            val first = parameterTypeList.params[0]
            if (first !is Parameter) {
                return emptyList()
            }
            val type = resolveParameterType(first)
            // Special case for void
            // Pattern: 'void f(void)' can be found in the C program.
            return if (type.cType() == VOID) {
                emptyList()
            } else {
                listOf(type)
            }
        }

        val paramTypes = mutableListOf<TypeDesc>()
        for (param in parameterTypeList.params) {
            when (param) {
                is Parameter -> {
                    val type = resolveParameterType(param)
                    paramTypes.add(type)
                }
                is ParameterVarArg -> {}
            }
        }

        return paramTypes
    }
}