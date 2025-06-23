package parser.nodes

import tokenizer.*


sealed class ExternalDeclaration {
    abstract fun begin(): Position
}
data class FunctionDeclarationNode(val function: FunctionNode): ExternalDeclaration() {
    override fun begin(): Position = function.begin()
}
data class GlobalDeclaration(val declaration: Declaration): ExternalDeclaration() {
    override fun begin(): Position = declaration.begin()
}

class ProgramNode(val filename: String, val nodes: MutableList<ExternalDeclaration>) {
    fun begin(): Position {
        if (nodes.isEmpty()) {
            return OriginalPosition(0, 0, filename)
        }

        return nodes.first().begin()
    }
}