package parser.nodes

import tokenizer.*
import types.*


abstract class Node {
    abstract fun<T> accept(visitor: NodeVisitor<T>): T
}

data class Declaration(val declspec: DeclarationSpecifier, val declarators: List<AnyDeclarator>): Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    fun resolveType(typeHolder: TypeHolder): List<String> {
        val type = declspec.resolveType(typeHolder)
        val vars = mutableListOf<String>()
        declarators.forEach {
            when (it) {
                is Declarator           -> {
                    it.resolveType(type, typeHolder)
                    vars.add(it.name())
                }
                is AssignmentDeclarator -> {
                    it.resolveType(type, typeHolder)
                    vars.add(it.name())
                }
                else -> TODO()
            }
        }
        return vars
    }
}

data class IdentNode(val str: Ident) : Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class NodePointer(val qualifiers: List<TypeQualifier>) : Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class FunctionNode(val specifier: DeclarationSpecifier, val declarator: Declarator, val body: Statement) : Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    fun name(): String {
        val varNode = declarator.directDeclarator.decl as VarDeclarator
        return varNode.ident.str()
    }

    fun functionDeclarator(): FunctionDeclarator {
        return declarator.directDeclarator.declarators[0] as FunctionDeclarator
    }

    fun resolveType(typeHolder: TypeHolder): CFunctionType {
        val type = specifier.resolveType(typeHolder)
        val s = functionDeclarator().resolveParams(typeHolder)
        val fnType = CFunctionType(name(), type, s)
        typeHolder.addFunctionType(name(), fnType)
        return fnType
    }
}

data class ProgramNode(val nodes: MutableList<Node>) : Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class StructField(val declspec: DeclarationSpecifier, val declarators: List<StructDeclarator>): Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    fun resolveType(typeHolder: TypeHolder): CType {
        TODO()
    }
}

class Enumerator(val ident: Ident, val expr: Node) : Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

object DummyNode : Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}