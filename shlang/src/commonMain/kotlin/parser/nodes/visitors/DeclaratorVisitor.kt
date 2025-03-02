package parser.nodes.visitors

import parser.nodes.*


interface DeclaratorVisitor<T> {
    fun visit(declarator: Declarator): T
    fun visit(initDeclarator: InitDeclarator): T
    fun visit(arrayDeclarator: ArrayDeclarator): T
    fun visit(emptyDeclarator: EmptyDeclarator): T
    fun visit(functionNode: FunctionNode): T
}