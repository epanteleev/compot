package parser.nodes

import common.assertion
import tokenizer.Position
import typedesc.TypeDesc
import typedesc.TypeHolder
import types.AbstractCFunction
import types.CFunctionType


data class DirectDeclarator(val decl: DirectDeclaratorEntry, val directDeclaratorParams: List<DirectDeclaratorParam>) {
    fun begin(): Position = decl.begin()

    private fun resolveAllDecl(baseType: TypeDesc, typeHolder: TypeHolder): TypeDesc {
        var currentType = baseType
        for (directDeclaratorParam in directDeclaratorParams.reversed()) {
            when (directDeclaratorParam) {
                is ArrayDeclarator -> {
                    currentType = directDeclaratorParam.resolveType(currentType, typeHolder)
                }

                is ParameterTypeList -> {
                    val abstractType = directDeclaratorParam.resolveType(currentType, typeHolder)
                    currentType = TypeDesc.from(CFunctionType(name(), abstractType.cType() as AbstractCFunction), abstractType.qualifiers())
                }

                is IdentifierList -> throw IllegalStateException("Identifier list is not supported")
            }
        }
        return currentType
    }

    private fun parameterTypeList(): ParameterTypeList {
        return directDeclaratorParams[0] as ParameterTypeList
    }

    fun resolveType(baseType: TypeDesc, typeHolder: TypeHolder): TypeDesc = when (decl) {
        is FunctionDeclarator -> {
            assertion(directDeclaratorParams.size == 1) { "Function pointer should have only one parameter" }
            val pointers = decl.declarator.pointers
            if (pointers.isEmpty()) {
                resolveAllDecl(baseType, typeHolder)
            } else {
                val fnDecl = parameterTypeList()
                val type = fnDecl.resolveType(baseType, typeHolder)
                decl.resolveType(type, typeHolder)
            }
        }
        is DirectVarDeclarator -> resolveAllDecl(baseType, typeHolder)
    }

    fun name(): String = decl.name()
}