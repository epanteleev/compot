package parser.nodes.visitors

import parser.nodes.*


interface DeclaratorVisitor<T> {
    fun visit(declarator: Declarator): T
    fun visit(initDeclarator: InitDeclarator): T
}