package parser.nodes

import typedesc.TypeDesc
import typedesc.TypeHolder
import parser.nodes.visitors.*
import tokenizer.Position
import tokenizer.tokens.Punctuator
import typedesc.DeclSpec
import typedesc.VarDescriptor


sealed class AnyParameter {
    abstract fun begin(): Position
    abstract fun<T> accept(visitor: ParameterVisitor<T>): T
}

sealed class AnyParamDeclarator {
   abstract fun resolveType(declSpec: DeclSpec, typeHolder: TypeHolder): TypeDesc
}

data class EmptyParamDeclarator(val where: Position) : AnyParamDeclarator() {
    override fun resolveType(declSpec: DeclSpec, typeHolder: TypeHolder): TypeDesc = declSpec.typeDesc
}

class ParamAbstractDeclarator(val abstractDeclarator: AbstractDeclarator) : AnyParamDeclarator() {
    override fun resolveType(declSpec: DeclSpec, typeHolder: TypeHolder): TypeDesc {
        return abstractDeclarator.resolveType(declSpec.typeDesc, typeHolder)
    }
}

class ParamDeclarator(val declarator: Declarator) : AnyParamDeclarator() {
    override fun resolveType(declSpec: DeclSpec, typeHolder: TypeHolder): TypeDesc {
        val varDesc = declarator.declareType(declSpec, typeHolder)
            ?: throw IllegalStateException("Typedef is not supported in function parameters")

        return varDesc.toTypeDesc()
    }
}

data class Parameter(val declspec: DeclarationSpecifier, val paramDeclarator: AnyParamDeclarator) : AnyParameter() {
    override fun begin(): Position = declspec.begin()
    override fun<T> accept(visitor: ParameterVisitor<T>): T = visitor.visit(this)

    private fun name(): String? {
        if (paramDeclarator !is ParamDeclarator) {
            return null
        }

        return paramDeclarator.declarator.directDeclarator.decl.name()
    }

    fun resolveType(typeHolder: TypeHolder): TypeDesc {
        val type = declspec.specifyType(typeHolder)
        return paramDeclarator.resolveType(type, typeHolder)
    }

    fun resolveVarDesc(typeHolder: TypeHolder): VarDescriptor? {
        val type = resolveType(typeHolder)
        val name = name() ?: return null
        return VarDescriptor(name, type.asType(begin()), type.qualifiers(), null)
    }
}

class ParameterVarArg(private val punctuation: Punctuator): AnyParameter() {
    override fun begin(): Position = punctuation.position()
    override fun<T> accept(visitor: ParameterVisitor<T>): T = visitor.visit(this)
}