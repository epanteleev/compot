package parser.nodes

import types.*
import tokenizer.Ident
import parser.nodes.visitors.*


abstract class AnyDeclarator: Node(), Resolvable {
    abstract fun<T> accept(visitor: DeclaratorVisitor<T>): T
}

data class AbstractDeclarator(val pointers: List<NodePointer>, val directAbstractDeclarator: List<Node>) : AnyDeclarator() {   //TODO
    override fun<T> accept(visitor: DeclaratorVisitor<T>): T = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        TODO()
    }
}

data class Declarator(val directDeclarator: DirectDeclarator, val pointers: List<NodePointer>): AnyDeclarator() {
    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)

    fun name(): String {
        return directDeclarator.name()
    }

    override fun resolveType(typeHolder: TypeHolder): CType {
        TODO()
    }

    fun resolveType(baseType: CType, typeHolder: TypeHolder): CType {
        var pointerType = baseType
        for (pointer in pointers) {
            pointerType = CPointerType(pointerType)
        }

        if (directDeclarator.decl is FunctionPointerDeclarator) {
            val functionPointerDeclarator = directDeclarator.decl
            val fnDecl = directDeclarator.declarators[0] as FunctionDeclarator
            val params = fnDecl.resolveParams(typeHolder)
            pointerType = CFunPointerType(pointerType, params)

            typeHolder.add(functionPointerDeclarator.declarator().name(), pointerType)
        } else {
            for (decl in directDeclarator.declarators) {
                when (decl) {
                    is ArrayDeclarator -> {
                        val size = decl.constexpr as NumNode
                        pointerType = CompoundType(CArrayType(pointerType, size.toLong.data.toInt()))
                    }
                    else -> throw IllegalStateException("Unknown declarator $decl")
                }
            }

            typeHolder.add(name(), pointerType)
        }


        return pointerType
    }
}

data class AssignmentDeclarator(val rvalue: Declarator, val lvalue: Expression): AnyDeclarator() {
    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        TODO("Not yet implemented")
    }

    fun name(): String {
        return rvalue.name()
    }

    fun resolveType(ctype: CType, typeHolder: TypeHolder): CType {
        return rvalue.resolveType(ctype, typeHolder)
    }
}

data class FunctionDeclarator(val params: List<AnyParameter>): AnyDeclarator() {
    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        params.forEach { it.resolveType(typeHolder) }
        return CType.UNKNOWN
    }

    fun params(): List<String> {
        return params.map {
            when (it) {
                is Parameter -> it.name()
                is ParameterVarArg -> "..."
                else -> throw IllegalStateException("Unknown parameter $it")

            }
        }
    }

    fun resolveParams(typeHolder: TypeHolder): List<CType> {
        return params.map { it.resolveType(typeHolder) }
    }
}

object EmptyDeclarator : AnyDeclarator() {
    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = CType.UNKNOWN
}

data class ArrayDeclarator(val constexpr: Expression) : AnyDeclarator() {
    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        TODO()
    }
}

data class StructDeclarator(val declarator: AnyDeclarator, val expr: Expression): AnyDeclarator() {
    override fun <T> accept(visitor: DeclaratorVisitor<T>): T {
        return visitor.visit(this)
    }

    fun name(): String {
        return when (declarator) {
            is Declarator -> declarator.name()
            else -> throw IllegalStateException("$declarator")
        }
    }

    override fun resolveType(typeHolder: TypeHolder): CType {
        TODO()
    }
}

data class DirectDeclarator(val decl: AnyDeclarator, val declarators: List<AnyDeclarator>): AnyDeclarator() {
    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)

    fun name(): String {
        return when (decl) {
            is VarDeclarator             -> decl.name()
            is FunctionPointerDeclarator -> decl.declarator().name()
            else -> throw IllegalStateException("$decl")
        }
    }

    override fun resolveType(typeHolder: TypeHolder): CType {
        TODO()
    }
}

data class VarDeclarator(val ident: Ident) : AnyDeclarator() {
    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)

    fun name(): String = ident.str()
    override fun resolveType(typeHolder: TypeHolder): CType {
        TODO()
    }
}

data class FunctionPointerDeclarator(val declarator: List<Node>): AnyDeclarator() {
    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)

    fun declarator(): Declarator {
        return declarator[0] as Declarator
    }

    override fun resolveType(typeHolder: TypeHolder): CType {
        TODO()
    }
}

data class FunctionPointerParamDeclarator(val declarator: Node, val params: Node): AnyDeclarator() {
    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        TODO()
    }
}

data class DirectFunctionDeclarator(val parameters: List<AnyParameter>) : AnyDeclarator() {
    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)
    override fun resolveType(typeHolder: TypeHolder): CType {
        TODO("Not yet implemented")
    }
}

data class DirectArrayDeclarator(val size: Node) : AnyDeclarator() {
    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)
    override fun resolveType(typeHolder: TypeHolder): CType {
        TODO("Not yet implemented")
    }
}