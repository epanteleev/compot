package parser.nodes.visitors

import parser.nodes.*


interface DeclaratorVisitor<T> {
    fun visit(declarator: Declarator): T
    fun visit(assignmentDeclarator: AssignmentDeclarator): T
    fun visit(arrayDeclarator: ArrayDeclarator): T
    fun visit(emptyDeclarator: EmptyDeclarator): T
    fun visit(structDeclarator: StructDeclarator): T
    fun visit(directDeclarator: DirectDeclarator): T
    fun visit(functionNode: FunctionNode): T
}