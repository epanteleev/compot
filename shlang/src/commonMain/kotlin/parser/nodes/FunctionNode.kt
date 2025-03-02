package parser.nodes

import common.assertion
import tokenizer.Position
import typedesc.TypeHolder
import typedesc.VarDescriptor
import types.CFunctionType

data class FunctionNode(val specifier: DeclarationSpecifier,
                        val declarator: Declarator,
                        val body: Statement) {

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
        val declspecType = specifier.specifyType(typeHolder, declarator.pointers)

        val type = declarator.directDeclarator.resolveType(declspecType.typeDesc, typeHolder)
        assertion(!specifier.isTypedef) { "typedef is not supported here" }

        val baseType = type.cType()
        assertion(baseType is CFunctionType) { "function type expected" }
        return@memoizeType typeHolder.addFunctionType(name(), VarDescriptor(type, declspecType.storageClass))
    }

    fun resolveType(typeHolder: TypeHolder): CFunctionType {
        return declareType(typeHolder).typeDesc.cType() as CFunctionType
    }
}