package parser.nodes

import types.*
import tokenizer.*
import parser.nodes.visitors.*



abstract class Node {
    fun<T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visit(this)
    }
}

abstract class UnclassifiedNode : Node() {
    abstract fun<T> accept(visitor: UnclassifiedNodeVisitor<T>): T
}

data class AbstractDeclarator(val pointers: List<NodePointer>, val directAbstractDeclarator: List<DirectDeclaratorParam>?) : UnclassifiedNode() {   //TODO
    override fun<T> accept(visitor: UnclassifiedNodeVisitor<T>): T = visitor.visit(this)

    fun resolveType(baseType: CType, typeHolder: TypeHolder): CType {
        var pointerType = baseType
        for (pointer in pointers) {
            pointerType = CPointerType(pointerType)
        }

        if (directAbstractDeclarator == null) {
            return pointerType
        }

        for (decl in directAbstractDeclarator) {
            when (decl) {
                is ArrayDeclarator -> decl.resolveType(pointerType, typeHolder)
                else -> throw IllegalStateException("Unknown declarator $decl")
            }
        }
        return pointerType
    }
}

data class Declaration(val declspec: DeclarationSpecifier, val declarators: List<AnyDeclarator>): UnclassifiedNode() {
    fun resolveType(typeHolder: TypeHolder): List<CType> {
        val type = declspec.resolveType(typeHolder)

        val vars = mutableListOf<CType>()
        for (it in declarators) when (it) {
            is Declarator -> {
                val resolved = it.resolveType(type, typeHolder)
                if (declspec.isTypedef) {
                    typeHolder.addStructType(it.name(), resolved.baseType()) //TODO BASETYPE???
                } else {
                    vars.add(resolved)
                }
            }
            is AssignmentDeclarator -> {
                vars.add(it.resolveType(type, typeHolder))
            }
            else -> TODO()
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
        return declarator.directDeclarator.decl.name()
    }

    fun functionDeclarator(): ParameterTypeList {
        return declarator.directDeclarator.declarators[0] as ParameterTypeList
    }

    fun resolveType(typeHolder: TypeHolder): CFunctionType {
        val type = specifier.resolveType(typeHolder)
        val s = declarator.resolveType(type, typeHolder) as CFunctionType
        typeHolder.addFunctionType(name(), s) //TODO already added???
        return s
    }

    override fun <T> accept(visitor: UnclassifiedNodeVisitor<T>): T = visitor.visit(this)
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

object DummyNode : UnclassifiedNode() {
    override fun <T> accept(visitor: UnclassifiedNodeVisitor<T>): T = visitor.visit(this)
}