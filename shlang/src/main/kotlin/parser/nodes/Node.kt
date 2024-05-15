package parser.nodes

import parser.nodes.visitors.*
import tokenizer.*
import types.*


abstract class Node {
    fun<T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visit(this)
    }
}

abstract class UnclassifiedNode : Node() {
    abstract fun<T> accept(visitor: UnclassifiedNodeVisitor<T>): T

}

data class Declaration(val declspec: DeclarationSpecifier, val declarators: List<AnyDeclarator>): UnclassifiedNode() {
    fun resolveType(typeHolder: TypeHolder): List<CType> {
        val type = declspec.resolveType(typeHolder)
        val vars = mutableListOf<CType>()
        declarators.forEach {
            when (it) {
                is Declarator           -> {
                    vars.add(it.resolveType(type, typeHolder))
                }
                is AssignmentDeclarator -> {
                    vars.add(it.resolveType(type, typeHolder))
                }
                else -> TODO()
            }
        }
        return vars
    }

    override fun <T> accept(visitor: UnclassifiedNodeVisitor<T>): T = visitor.visit(this)
}

data class IdentNode(private val str: Identifier) : UnclassifiedNode() {
    fun str(): String = str.str()
    override fun <T> accept(visitor: UnclassifiedNodeVisitor<T>): T = visitor.visit(this)
}

data class NodePointer(val qualifiers: List<TypeQualifier>) : UnclassifiedNode() {
    override fun <T> accept(visitor: UnclassifiedNodeVisitor<T>): T = visitor.visit(this)
}

data class FunctionNode(val specifier: DeclarationSpecifier,
                        val declarator: Declarator,
                        val body: Statement) : UnclassifiedNode() {
    fun name(): String {
        val varNode = declarator.directDeclarator.decl as VarDeclarator
        return varNode.ident.str()
    }

    fun functionDeclarator(): ParameterTypeList {
        return declarator.directDeclarator.declarators[0] as ParameterTypeList
    }

    fun resolveType(typeHolder: TypeHolder): CFunctionType {
        val type = specifier.resolveType(typeHolder)
        val s = functionDeclarator().resolveParams(typeHolder)
        val fnType = CFunctionType(name(), type, s, functionDeclarator().isVarArg())
        typeHolder.addFunctionType(name(), fnType)
        return fnType
    }

    override fun <T> accept(visitor: UnclassifiedNodeVisitor<T>): T = visitor.visit(this)
}

data class ProgramNode(val nodes: MutableList<Node>) : UnclassifiedNode() {
    override fun <T> accept(visitor: UnclassifiedNodeVisitor<T>): T = visitor.visit(this)
}

data class StructField(val declspec: DeclarationSpecifier, val declarators: List<StructDeclarator>): UnclassifiedNode(){
    override fun <T> accept(visitor: UnclassifiedNodeVisitor<T>): T = visitor.visit(this)
}

class Enumerator(val ident: Identifier, val expr: Node) : UnclassifiedNode() {
    override fun <T> accept(visitor: UnclassifiedNodeVisitor<T>): T = visitor.visit(this)
}

object DummyNode : UnclassifiedNode() {
    override fun <T> accept(visitor: UnclassifiedNodeVisitor<T>): T = visitor.visit(this)
}