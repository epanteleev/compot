package parser.nodes

import parser.nodes.visitors.ExpressionVisitor
import tokenizer.Position
import typedesc.TypeHolder
import types.CStringLiteral
import types.CType
import types.InitializerType

sealed class InitializerListEntry {
    protected var type: CType? = null

    protected inline fun<reified T: CType> memoize(closure: () -> T): T {
        if (type != null) {
            return type as T
        }
        type = closure()
        return type as T
    }

    abstract fun begin(): Position
    abstract fun resolveType(typeHolder: TypeHolder): CType
}

class SingleInitializer(val expr: Initializer) : InitializerListEntry() {
    override fun begin(): Position = expr.begin()

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        return@memoize expr.resolveType(typeHolder)
    }
}

class DesignationInitializer(val designation: Designation, val initializer: Initializer) : InitializerListEntry() {
    override fun begin(): Position = designation.begin()

    override fun resolveType(typeHolder: TypeHolder): CType = memoize {
        return@memoize initializer.resolveType(typeHolder)
    }
}

class InitializerList(private val begin: Position, val initializers: List<InitializerListEntry>) {
    private var type: CType? = null

    private inline fun<reified T: CType> memoize(closure: () -> T): T {
        if (type != null) {
            return type as T
        }
        type = closure()
        return type as T
    }

    fun begin(): Position = begin

    fun resolveType(typeHolder: TypeHolder): CType = memoize {
        val types = initializers.map { it.resolveType(typeHolder) }

        val baseTypes = arrayListOf<CType>()
        for (i in initializers.indices) {
            baseTypes.add(types[i])
        }
        if (baseTypes.size == 1 && baseTypes[0] is CStringLiteral) {
            return@memoize baseTypes[0] //TODO is it needed?
        } else {
            return@memoize InitializerType(baseTypes)
        }
    }

    fun length(): Int = initializers.size
}