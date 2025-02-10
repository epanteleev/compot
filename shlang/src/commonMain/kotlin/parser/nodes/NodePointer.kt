package parser.nodes

import tokenizer.Position
import typedesc.TypeQualifier

class NodePointer(val begin: Position, val qualifiers: List<TypeQualifierNode>) {
    fun begin(): Position = begin

    fun property(): Set<TypeQualifier> {
        return qualifiers.mapTo(hashSetOf()) { it.qualifier() }
    }
}