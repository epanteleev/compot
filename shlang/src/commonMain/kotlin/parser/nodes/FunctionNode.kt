package parser.nodes

import tokenizer.Position
import typedesc.TypeHolder
import typedesc.VarDescriptor
import types.CFunctionType

class FunctionNode(val specifier: DeclarationSpecifier, val declarator: Declarator, val body: Statement) {
    private var cachedType: VarDescriptor? = null
    private fun memoizeType(type: () -> VarDescriptor): VarDescriptor {
        if (cachedType == null) {
            cachedType = type()
        }

        return cachedType!!
    }

    fun begin(): Position = specifier.begin()

    fun name(): String {
        return declarator.directDeclarator.decl.name()
    }

    fun functionDeclarator(): ParameterTypeList {
        return declarator.directDeclarator.directDeclaratorParams[0] as ParameterTypeList
    }

    fun declareType(typeHolder: TypeHolder): VarDescriptor = memoizeType {
        val declspecType = specifier.specifyType(typeHolder)

        return@memoizeType declarator.declareType(declspecType, typeHolder)
            ?: throw IllegalStateException("Function type is not specified")
    }

    fun resolveType(typeHolder: TypeHolder): CFunctionType {
        return declareType(typeHolder).typeDesc.cType() as CFunctionType
    }
}