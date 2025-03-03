package parser.nodes

import tokenizer.Position
import typedesc.TypeHolder
import typedesc.VarDescriptor

data class Declaration(val declspec: DeclarationSpecifier, private val declarators: List<AnyDeclarator>) {
    fun begin(): Position = declspec.begin()

    fun declarators(): List<AnyDeclarator> {
        return declarators
    }

    fun specifyType(typeHolder: TypeHolder): VarDescriptor {
        val varDesc = declspec.specifyType(typeHolder)
        for (it in declarators) {
            it.declareType(varDesc, typeHolder)
        }

        return declspec.specifyType(typeHolder)
    }
}