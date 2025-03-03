package parser.nodes

import tokenizer.Position
import typedesc.TypeHolder

data class Declaration(val declspec: DeclarationSpecifier, private val declarators: List<AnyDeclarator>) {
    fun begin(): Position = declspec.begin()

    fun declarators(): List<AnyDeclarator> {
        return declarators
    }

    fun specifyType(typeHolder: TypeHolder) {
        val varDesc = declspec.specifyType(typeHolder)
        for (it in declarators) {
            it.declareType(varDesc, typeHolder)
        }
    }
}