package parser.nodes

import types.*
import parser.nodes.visitors.*


abstract class AnyDeclarator: Node() {
    abstract fun<T> accept(visitor: DeclaratorVisitor<T>): T

    abstract fun resolveType(baseType: CType, typeHolder: TypeHolder): CType
}

data class AbstractDeclarator(val pointers: List<NodePointer>, val directAbstractDeclarator: List<DirectDeclaratorParam>?) : AnyDeclarator() {   //TODO
    override fun<T> accept(visitor: DeclaratorVisitor<T>): T = visitor.visit(this)

    override fun resolveType(baseType: CType, typeHolder: TypeHolder): CType {
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

data class Declarator(val directDeclarator: DirectDeclarator, val pointers: List<NodePointer>): AnyDeclarator() {
    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)

    fun name(): String {
        return directDeclarator.name()
    }

    override fun resolveType(baseType: CType, typeHolder: TypeHolder): CType {
        var pointerType = baseType
        for (pointer in pointers) {
            pointerType = CPointerType(pointerType)
        }

        pointerType = directDeclarator.resolveType(pointerType, typeHolder)
        typeHolder.addVar(name(), pointerType)
        return pointerType
    }
}

data class AssignmentDeclarator(val declarator: Declarator, val rvalue: Expression): AnyDeclarator() { //TODO rename
    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)

    fun name(): String {
        return declarator.name()
    }

    override fun resolveType(baseType: CType, typeHolder: TypeHolder): CType {
        return declarator.resolveType(baseType, typeHolder)
    }
}

object EmptyDeclarator : AnyDeclarator() {
    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)

    override fun resolveType(baseType: CType, typeHolder: TypeHolder): CType {
        return CType.UNKNOWN
    }
}

data class StructDeclarator(val declarator: AnyDeclarator, val expr: Expression): AnyDeclarator() {
    override fun <T> accept(visitor: DeclaratorVisitor<T>): T {
        return visitor.visit(this)
    }

    override fun resolveType(baseType: CType, typeHolder: TypeHolder): CType {
        require(expr is EmptyExpression) {
            "unsupported expression in struct declarator $expr"
        }

        return declarator.resolveType(baseType, typeHolder)
    }

    fun name(): String {
        return when (declarator) {
            is Declarator -> declarator.name()
            else -> throw IllegalStateException("$declarator")
        }
    }
}

data class DirectDeclarator(val decl: DirectDeclaratorFirstParam, val declarators: List<DirectDeclaratorParam>): AnyDeclarator() {
    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)

    private fun resolveAllDecl(baseType: CType, typeHolder: TypeHolder): CType {
        var pointerType = baseType
        for (decl in declarators) {
            when (decl) {
                is ArrayDeclarator -> {
                    pointerType = decl.resolveType(pointerType, typeHolder)
                }

                is ParameterTypeList -> {
                    val abstractType = decl.resolveType(pointerType, typeHolder)
                    pointerType = CFunctionType(name(), abstractType)
                }

                else -> throw IllegalStateException("Unknown declarator $decl")
            }
        }
        return pointerType
    }

    override fun resolveType(baseType: CType, typeHolder: TypeHolder): CType {
        when (decl) {
            is FunctionPointerDeclarator -> {
                val fnDecl = declarators[0] as ParameterTypeList
                val type = fnDecl.resolveType(baseType, typeHolder)
                return CFunPointerType(type)
            }
            is DirectVarDeclarator -> {
                return resolveAllDecl(baseType, typeHolder)
            }
            else -> {
                return CType.UNKNOWN
            }
        }
    }

    fun name(): String = decl.name()
}