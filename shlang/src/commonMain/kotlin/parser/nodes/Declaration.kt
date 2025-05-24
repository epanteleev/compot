package parser.nodes

import sema.SemanticAnalysis
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
        val declSpec = declspec.accept(SemanticAnalysis(typeHolder))
        val sema = SemanticAnalysis(typeHolder)
        for (declarator in declarators) {
            val varDesc = sema.declareVar(declarator, declSpec) ?: continue
            varDescs.add(varDesc)
        }

        return varDescs
    }

    companion object {
        fun create(typeHolder: TypeHolder, declspec: DeclarationSpecifier, declarators: List<AnyDeclarator>): Declaration {
            val declSpec = declspec.accept(SemanticAnalysis(typeHolder))
            if (declSpec.storageClass != StorageClass.TYPEDEF) {
                return Declaration(declspec, declarators, false)
            }

            val sema = SemanticAnalysis(typeHolder)
            for (declarator in declarators) {
                if (declarator !is Declarator) {
                    continue
                }

                val typedef = sema.resolveTypedef(declarator, declSpec)
                if (typedef != null) {
                    typeHolder.addTypedef(typedef)
                }
            }
            return Declaration(declspec, declarators, true)
        }
    }
}