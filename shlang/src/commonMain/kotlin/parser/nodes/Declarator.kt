package parser.nodes

import types.*
import tokenizer.Identifier
import parser.nodes.visitors.*


abstract class AnyDeclarator: Node() {
    abstract fun<T> accept(visitor: DeclaratorVisitor<T>): T
}

data class AbstractDeclarator(val pointers: List<NodePointer>, val directAbstractDeclarator: List<DirectDeclaratorParam>?) : AnyDeclarator() {   //TODO
    override fun<T> accept(visitor: DeclaratorVisitor<T>): T = visitor.visit(this)

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
                is ArrayDeclarator -> {
                    val size = decl.constexpr as NumNode
                    pointerType = CompoundType(CArrayType(pointerType, size.toLong.data.toInt()))
                }
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

    fun resolveType(baseType: CType, typeHolder: TypeHolder): CType {
        var pointerType = baseType
        for (pointer in pointers) {
            pointerType = CPointerType(pointerType)
        }

        if (directDeclarator.decl is FunctionPointerDeclarator) {
            val functionPointerDeclarator = directDeclarator.decl
            val fnDecl = directDeclarator.declarators[0] as ParameterTypeList
            val params = fnDecl.resolveParams(typeHolder)
            pointerType = CFunPointerType(pointerType, params)

            typeHolder.add(functionPointerDeclarator.declarator().name(), pointerType)
        } else {
            for (decl in directDeclarator.declarators) {
                when (decl) {
                    is ArrayDeclarator -> {
                        if (decl.constexpr is EmptyExpression) {
                            pointerType = CPointerType(pointerType)
                            continue
                        }

                        val size = decl.constexpr as NumNode //TODO evaluate
                        pointerType = CompoundType(CArrayType(pointerType, size.toLong.data.toInt()))
                    }
                    is ParameterTypeList -> {
                        val params = decl.resolveParams(typeHolder)
                        pointerType = CFunctionType(directDeclarator.name(), pointerType, params, decl.isVarArg())
                    }
                    else -> throw IllegalStateException("Unknown declarator $decl")
                }
            }

            typeHolder.add(name(), pointerType)
        }


        return pointerType
    }
}

data class AssignmentDeclarator(val declarator: Declarator, val rvalue: Expression): AnyDeclarator() { //TODO rename
    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)

    fun name(): String {
        return declarator.name()
    }

    fun resolveType(ctype: CType, typeHolder: TypeHolder): CType {
        return declarator.resolveType(ctype, typeHolder)
    }
}

object EmptyDeclarator : AnyDeclarator() {
    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)
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
}

data class DirectDeclarator(val decl: DirectDeclaratorFirstParam, val declarators: List<DirectDeclaratorParam>): AnyDeclarator() {
    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)

    fun name(): String = decl.name()
}

data class VarDeclarator(val ident: Identifier) : AnyDeclarator() {
    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)

    fun name(): String = ident.str()
}

data class FunctionPointerParamDeclarator(val declarator: Node, val params: Node): AnyDeclarator() {
    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)
}

data class DirectFunctionDeclarator(val parameters: List<AnyParameter>) : AnyDeclarator() {
    override fun<T> accept(visitor: DeclaratorVisitor<T>) = visitor.visit(this)
}