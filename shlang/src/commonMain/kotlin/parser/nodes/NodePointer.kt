package parser.nodes

import tokenizer.Position
import typedesc.TypeQualifier

class NodePointer(val begin: Position, val qualifiers: List<TypeQualifierNode>) {
    fun begin(): Position = begin

    fun property(): List<TypeQualifier> {
        return qualifiers.map { it.qualifier() }
    }
}