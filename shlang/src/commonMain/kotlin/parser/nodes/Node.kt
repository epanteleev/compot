package parser.nodes

import types.*
import common.assertion
import parser.nodes.visitors.*
import tokenizer.tokens.Identifier
import typedesc.TypeDesc
import typedesc.TypeHolder
import typedesc.TypeQualifier
import typedesc.VarDescriptor


sealed class Node {
    fun<T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visit(this)
    }
}

sealed class UnclassifiedNode : Node() {
    abstract fun<T> accept(visitor: UnclassifiedNodeVisitor<T>): T
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