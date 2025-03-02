package parser.nodes.visitors

import parser.nodes.*


interface NodeVisitor<T> : ExpressionVisitor<T>,
    DeclaratorVisitor<T>,
    StatementVisitor<T>,
    TypeNodeVisitor<T>,
    TypeSpecifierVisitor<T>,
    ParameterVisitor<T>,
    DirectDeclaratorParamVisitor<T> {
    fun visit(node: Node): T {
        return when (node) {
            is AnyDeclarator    -> node.accept(this as DeclaratorVisitor<T>)
            is DirectDeclaratorParam -> node.accept(this as DirectDeclaratorParamVisitor<T>)
        }
    }
}