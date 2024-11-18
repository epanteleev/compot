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
            is Expression       -> node.accept(this as ExpressionVisitor<T>)
            is Statement        -> node.accept(this as StatementVisitor<T>)
            is TypeSpecifier    -> node.accept(this as TypeSpecifierVisitor<T>)
            is AnyTypeNode      -> node.accept(this as TypeNodeVisitor<T>)
            is AnyParameter     -> node.accept(this as ParameterVisitor<T>)
            is DirectDeclaratorParam -> node.accept(this as DirectDeclaratorParamVisitor<T>)
        }
    }
}