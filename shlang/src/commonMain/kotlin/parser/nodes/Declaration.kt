package parser.nodes

import tokenizer.Position
import typedesc.TypeHolder
import types.CFunctionType

data class Declaration(val declspec: DeclarationSpecifier, private val declarators: List<AnyDeclarator>) {
    fun begin(): Position = declspec.begin()

    fun declarators(): List<AnyDeclarator> {
        return declarators
    }

    fun specifyType(typeHolder: TypeHolder) {
        val varDesc = declspec.specifyType(typeHolder)
        for (it in declarators) {
            val varDesc = it.declareType(varDesc, typeHolder) ?: continue
            val baseType = varDesc.cType()

            if (baseType is CFunctionType) {
                // declare extern function or function without body
                typeHolder.addFunctionType(varDesc)
            } else {
                typeHolder.addVar(varDesc)
            }
        }
    }
}