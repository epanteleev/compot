package parser.nodes

import types.*
import tokenizer.*
import parser.nodes.visitors.*


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

    fun resolveType(baseType: TypeDesc): TypeDesc {
        var pointerType = baseType.baseType()
        for (pointer in pointers) {
            pointerType = CPointerT(pointerType, pointer.property().toSet())
        }

        return TypeDesc.from(pointerType)
    }
}

data class Declaration(val declspec: DeclarationSpecifier, private val declarators: List<AnyDeclarator>): UnclassifiedNode() {
    fun specifyType(typeHolder: TypeHolder) {
        declspec.specifyType(typeHolder, listOf()) // Important: define new type here
        for (it in declarators) {
            it.declareType(declspec, typeHolder)
        }
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
                    currentType = CFunctionType(CBaseFunctionType(name(), abstractType.baseType), abstractType.properties)
                }

                else -> throw IllegalStateException("Unknown declarator $decl")
            }
        }
        return currentType
    }

    fun resolveType(baseType: TypeDesc, typeHolder: TypeHolder): TypeDesc = when (decl) {
        is FunctionPointerDeclarator -> {
            val fnDecl = directDeclaratorParams[0] as ParameterTypeList
            val type = fnDecl.resolveType(baseType, typeHolder)
            TypeDesc.from(CFunPointerT(type.baseType, emptySet()))
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