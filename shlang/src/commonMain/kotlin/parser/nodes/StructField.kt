package parser.nodes

import tokenizer.Position
import typedesc.TypeHolder
import typedesc.DeclSpec
import typedesc.VarDescriptor

sealed class AnyStructDeclaratorItem {
    abstract fun begin(): Position
    abstract fun declareType(varDesc: DeclSpec, typeHolder: TypeHolder): VarDescriptor
}

class StructDeclaratorItem(val expr: Declarator): AnyStructDeclaratorItem() {
    override fun begin(): Position = expr.begin()

    override fun declareType(varDesc: DeclSpec, typeHolder: TypeHolder): VarDescriptor {
        return expr.declareType(varDesc, typeHolder)
    }

}
class EmptyStructDeclaratorItem(private val where: Position): AnyStructDeclaratorItem() {
    override fun begin(): Position = where

    override fun declareType(varDesc: DeclSpec, typeHolder: TypeHolder): VarDescriptor {
        throw IllegalStateException("Empty declarator is not supported")
    }
}

data class StructDeclarator(val declarator: AnyStructDeclaratorItem, val expr: Expression) {
    private var cachedType: VarDescriptor? = null

    private fun memoizeType(type: () -> VarDescriptor): VarDescriptor {
        if (cachedType == null) {
            cachedType = type()
        }

        return cachedType!!
    }

    fun begin(): Position = declarator.begin()

    fun declareType(varDesc: DeclSpec, typeHolder: TypeHolder): VarDescriptor = memoizeType {
        if(expr !is EmptyExpression) {
            println("Warning: bit field is not supported")
        }

        return@memoizeType declarator.declareType(varDesc, typeHolder)
    }

    fun name(): String = when(declarator) {
        is StructDeclaratorItem -> declarator.expr.name()
        is EmptyStructDeclaratorItem -> ""
    }
}

class StructField(val declspec: DeclarationSpecifier, val declarators: List<StructDeclarator>) {
    fun begin(): Position = declspec.begin()
}