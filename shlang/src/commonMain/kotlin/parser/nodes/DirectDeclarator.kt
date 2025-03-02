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
        for (decl in directDeclaratorParams.reversed()) {
            when (decl) {
                is ArrayDeclarator -> {
                    currentType = decl.resolveType(currentType, typeHolder)
                }

                is ParameterTypeList -> {
                    val abstractType = decl.resolveType(currentType, typeHolder)
                    currentType = TypeDesc.from(CFunctionType(name(), abstractType.cType() as AbstractCFunction), abstractType.qualifiers())
                }

                else -> throw IllegalStateException("Unknown declarator $decl")
            }
        }
        return currentType
    }

    fun resolveType(baseType: TypeDesc, typeHolder: TypeHolder): TypeDesc = when (decl) {
        is FunctionDeclarator -> {
            assertion(directDeclaratorParams.size == 1) { "Function pointer should have only one parameter" }
            val fnDecl = directDeclaratorParams[0] as ParameterTypeList
            val pointers = decl.declarator.pointers
            if (pointers.isEmpty()) {
                resolveAllDecl(baseType, typeHolder)
            } else {
                val type = fnDecl.resolveType(baseType, typeHolder)
                decl.resolveType(type, typeHolder)
            }
        }
        is DirectVarDeclarator -> resolveAllDecl(baseType, typeHolder)
    }

    fun name(): String = decl.name()
}