package parser.nodes

import tokenizer.Position
import typedesc.TypeHolder
import typedesc.VarDescriptor


data class StructDeclarator(val declarator: AnyDeclarator, val expr: Expression) {
    private var cachedType: VarDescriptor? = null

    private fun memoizeType(type: () -> VarDescriptor): VarDescriptor {
        if (cachedType == null) {
            cachedType = type()
        }

        return cachedType!!
    }

    fun begin(): Position = declarator.begin()

    fun declareType(declspec: DeclarationSpecifier, typeHolder: TypeHolder): VarDescriptor = memoizeType {
        if(expr !is EmptyExpression) {
            println("Warning: bit field is not supported")
        }

        return@memoizeType declarator.declareType(declspec, typeHolder)
    }

    fun name(): String = declarator.name()
}

class StructField(val declspec: DeclarationSpecifier, val declarators: List<StructDeclarator>) {
    fun begin(): Position = declspec.begin()
}