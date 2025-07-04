package parser.nodes

import tokenizer.Position
import typedesc.TypeHolder
import typedesc.VarDescriptor
import types.CFunctionType


class FunctionNode internal constructor(val typeHolder: TypeHolder, val varDescriptor: VarDescriptor, val specifier: DeclarationSpecifier, val declarator: Declarator, val body: CompoundStatement) {
    fun begin(): Position = specifier.begin()

    fun name(): String {
        return declarator.directDeclarator.decl.name()
    }

    fun parameterTypeList(): ParameterTypeList {
        val decl = declarator.directDeclarator.decl
        if (decl !is FunctionDeclarator) {
            val first = declarator.directDeclarator.directDeclaratorParams.firstOrNull()
            if (first !is ParameterTypeList) {
                throw IllegalStateException("Expected ParameterTypeList, but found: $first in ${begin()}")
            }

            return first
        }

        val directDeclaratorParams = decl.declarator.directDeclarator.directDeclaratorParams
        if (directDeclaratorParams.size == 1 && directDeclaratorParams[0] is ParameterTypeList) {
            return directDeclaratorParams[0] as ParameterTypeList
        }
        return declarator.directDeclarator.directDeclaratorParams[0] as ParameterTypeList
    }

    fun cFunctionType(): CFunctionType {
        return varDescriptor.cType() as CFunctionType
    }
}