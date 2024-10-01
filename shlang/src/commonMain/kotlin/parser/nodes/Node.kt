package parser.nodes

import types.*
import common.assertion
import parser.nodes.visitors.*
import tokenizer.tokens.Identifier
import typedesc.TypeDesc
import typedesc.TypeHolder
import typedesc.TypeQualifier


sealed class Node {
    fun<T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visit(this)
    }
}

sealed class UnclassifiedNode : Node() {
    abstract fun<T> accept(visitor: UnclassifiedNodeVisitor<T>): T
}

data class AbstractDeclarator(val pointers: List<NodePointer>, val directAbstractDeclarator: List<DirectDeclaratorParam>?) : UnclassifiedNode() {   //TODO
    override fun<T> accept(visitor: UnclassifiedNodeVisitor<T>): T = visitor.visit(this)

    fun resolveType(baseType: TypeDesc, typeHolder: TypeHolder): TypeDesc {
        var pointerType = baseType.cType()
        for (pointer in pointers) {
            pointerType = CPointer(pointerType, pointer.property().toSet())
        }
        var typeDesc = TypeDesc.from(pointerType)
        if (directAbstractDeclarator == null) {
            return typeDesc
        }

        for (decl in directAbstractDeclarator.reversed()) {
            typeDesc = when (decl) {
                is ArrayDeclarator -> decl.resolveType(typeDesc, typeHolder)
                else -> throw IllegalStateException("Unknown declarator $decl")
            }
        }

        return typeDesc
    }
}

data class Declaration(val declspec: DeclarationSpecifier, private val declarators: List<AnyDeclarator>): UnclassifiedNode() {
    fun specifyType(typeHolder: TypeHolder) {
        for (it in declarators) {
            it.declareType(declspec, typeHolder)
        }
        declspec.specifyType(typeHolder, listOf())
    }

    fun nonTypedefDeclarators(): List<AnyDeclarator> {
        if (declspec.isTypedef) {
            return listOf()
        }
        return declarators
    }

    fun declarators(): List<AnyDeclarator> {
        return declarators
    }

    override fun <T> accept(visitor: UnclassifiedNodeVisitor<T>): T = visitor.visit(this)
}

data class DirectDeclarator(val decl: DirectDeclaratorFirstParam, val directDeclaratorParams: List<DirectDeclaratorParam>): UnclassifiedNode() {
    override fun<T> accept(visitor: UnclassifiedNodeVisitor<T>) = visitor.visit(this)

    private fun resolveAllDecl(baseType: TypeDesc, typeHolder: TypeHolder): TypeDesc {
        var currentType = baseType
        for (decl in directDeclaratorParams.reversed()) {
            when (decl) {
                is ArrayDeclarator -> {
                    currentType = decl.resolveType(currentType, typeHolder)
                }

                is ParameterTypeList -> {
                    val abstractType = decl.resolveType(currentType, typeHolder)
                    currentType = TypeDesc.from(CFunctionType(name(), abstractType.cType() as AbstractCFunction), abstractType.qualifiers())
                }

                else -> throw IllegalStateException("Unknown declarator $decl")
            }
        }
        return currentType
    }

    fun resolveType(baseType: TypeDesc, typeHolder: TypeHolder): TypeDesc = when (decl) {
        is FunctionPointerDeclarator -> {
            assertion(directDeclaratorParams.size == 1) { "Function pointer should have only one parameter" }
            val fnDecl = directDeclaratorParams[0] as ParameterTypeList
            val type = fnDecl.resolveType(baseType, typeHolder)
            decl.resolveType(type, typeHolder)
        }
        is DirectVarDeclarator -> resolveAllDecl(baseType, typeHolder)
    }

    fun name(): String = decl.name()
}

data class IdentNode(private val str: Identifier) : UnclassifiedNode() {
    fun str(): String = str.str()
    override fun <T> accept(visitor: UnclassifiedNodeVisitor<T>): T = visitor.visit(this)
}

data class NodePointer(val qualifiers: List<TypeQualifierNode>) : UnclassifiedNode() {
    override fun <T> accept(visitor: UnclassifiedNodeVisitor<T>): T = visitor.visit(this)

    fun property(): List<TypeQualifier> {
        return qualifiers.map { it.qualifier() }
    }
}

data class ProgramNode(val nodes: MutableList<Node>) : UnclassifiedNode() {
    override fun <T> accept(visitor: UnclassifiedNodeVisitor<T>): T = visitor.visit(this)
}

data class StructField(val declspec: DeclarationSpecifier, val declarators: List<StructDeclarator>): UnclassifiedNode(){
    override fun <T> accept(visitor: UnclassifiedNodeVisitor<T>): T = visitor.visit(this)
}

class Enumerator(val ident: Identifier, val constExpr: Expression) : UnclassifiedNode() {
    fun name() = ident.str()
    override fun <T> accept(visitor: UnclassifiedNodeVisitor<T>): T = visitor.visit(this)
}

data object DummyNode : UnclassifiedNode() {
    override fun <T> accept(visitor: UnclassifiedNodeVisitor<T>): T = visitor.visit(this)
}