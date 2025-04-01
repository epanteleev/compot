package parser.nodes

import tokenizer.Position
import typedesc.TypeHolder
import typedesc.VarDescriptor


data class Declaration(val declspec: DeclarationSpecifier, private val declarators: List<AnyDeclarator>) {
    fun begin(): Position = declspec.begin()

    fun declarators(): List<AnyDeclarator> {
        return declarators
    }

    fun declareVars(typeHolder: TypeHolder): List<VarDescriptor> {
        val varDescs = arrayListOf<VarDescriptor>()
        val declSpec = declspec.specifyType(typeHolder)
        for (declarator in declarators) {
            val varDesc = declarator.declareType(declSpec, typeHolder) ?: continue
            varDescs.add(varDesc)
        }

        return varDescs
    }
}