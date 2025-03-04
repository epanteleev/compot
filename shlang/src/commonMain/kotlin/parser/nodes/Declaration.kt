package parser.nodes

import tokenizer.Position
import typedesc.TypeHolder


data class Declaration(val declspec: DeclarationSpecifier, private val declarators: List<AnyDeclarator>) {
    fun begin(): Position = declspec.begin()

    fun declarators(): List<AnyDeclarator> {
        return declarators
    }

    fun specifyType(typeHolder: TypeHolder) {
        val declSpec = declspec.specifyType(typeHolder)
        for (declarator in declarators) {
            val varDesc = declarator.declareType(declSpec, typeHolder) ?: continue
            typeHolder.addVar(varDesc)
        }
    }
}