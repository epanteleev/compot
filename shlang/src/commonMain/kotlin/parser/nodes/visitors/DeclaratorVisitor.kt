package parser.nodes.visitors

import parser.nodes.*


interface DeclaratorVisitor<T> {
    fun visit(abstractDeclarator: AbstractDeclarator): T
    fun visit(declarator: Declarator): T
    fun visit(assignmentDeclarator: AssignmentDeclarator): T
    fun visit(functionPointerParamDeclarator: FunctionPointerParamDeclarator): T
    fun visit(arrayDeclarator: ArrayDeclarator): T
    fun visit(directFunctionDeclarator: DirectFunctionDeclarator): T
    fun visit(directArrayDeclarator: DirectArrayDeclarator): T
    fun visit(emptyDeclarator: EmptyDeclarator): T
    fun visit(varDeclarator: VarDeclarator): T
    fun visit(structDeclarator: StructDeclarator): T
    fun visit(directDeclarator: DirectDeclarator): T
}