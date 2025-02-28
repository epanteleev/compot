package parser.nodes

import tokenizer.Position
import tokenizer.tokens.Identifier
import typedesc.TypeDesc
import typedesc.TypeHolder
import types.AbstractCFunction
import types.CPointer


// Special case for first parameter of DirectDeclarator
sealed class DirectDeclaratorEntry {
    abstract fun begin(): Position
    abstract fun name(): String
}

data class FunctionDeclarator(val declarator: Declarator): DirectDeclaratorEntry() {
    override fun begin(): Position = declarator.begin()

    override fun name(): String = declarator.name()

    fun resolveType(typeDesc: TypeDesc, typeHolder: TypeHolder): TypeDesc {
        val cType = if (typeDesc.cType() is AbstractCFunction) {
            TypeDesc.from(CPointer(typeDesc.cType() as AbstractCFunction, setOf()), listOf())
        } else {
            typeDesc
        }
        return declarator.directDeclarator.resolveType(cType, typeHolder)
    }
}

data class DirectVarDeclarator(val ident: Identifier): DirectDeclaratorEntry() {
    override fun begin(): Position = ident.position()
    override fun name(): String = ident.str()
}