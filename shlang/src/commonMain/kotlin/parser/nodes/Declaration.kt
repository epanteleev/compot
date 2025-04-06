package parser.nodes

import tokenizer.Position
import typedesc.StorageClass
import typedesc.TypeHolder
import typedesc.VarDescriptor


class Declaration private constructor(val declspec: DeclarationSpecifier, private val declarators: List<AnyDeclarator>, val isTypedef: Boolean) {
    fun begin(): Position = declspec.begin()

    fun declarators(): List<AnyDeclarator> {
        return declarators
    }

    fun declareVars(typeHolder: TypeHolder): List<VarDescriptor> {
        if (isTypedef) {
            return emptyList()
        }

        val varDescs = arrayListOf<VarDescriptor>()
        val declSpec = declspec.specifyType(typeHolder)
        for (declarator in declarators) {
            val varDesc = declarator.declareVar(declSpec, typeHolder) ?: continue
            varDescs.add(varDesc)
        }

        return varDescs
    }

    companion object {
        fun create(typeHolder: TypeHolder, declspec: DeclarationSpecifier, declarators: List<AnyDeclarator>): Declaration {
            val declSpec = declspec.specifyType(typeHolder)
            if (declSpec.storageClass != StorageClass.TYPEDEF) {
                return Declaration(declspec, declarators, false)
            }

            for (declarator in declarators) {
                if (declarator !is Declarator) {
                    continue
                }

                declarator.resolveTypedef(declSpec, typeHolder)
            }
            return Declaration(declspec, declarators, true)
        }
    }
}