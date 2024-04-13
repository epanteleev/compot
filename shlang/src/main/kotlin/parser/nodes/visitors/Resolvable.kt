package parser.nodes.visitors

import types.CType
import types.TypeHolder

interface Resolvable {
    fun resolveType(typeHolder: TypeHolder): CType
}