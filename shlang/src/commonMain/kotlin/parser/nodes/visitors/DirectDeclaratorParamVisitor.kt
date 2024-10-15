package parser.nodes.visitors

import parser.nodes.*


interface DirectDeclaratorParamVisitor<T> {
    fun visit(parameters: ParameterTypeList): T
    fun visit(arrayDeclarator: ArrayDeclarator): T
    fun visit(functionPointer: FunctionPointerDeclarator): T
    fun visit(varDecl: DirectVarDeclarator): T
    fun visit(identifierList: IdentifierList): T
    fun visit(abstractDeclarator: AbstractDeclarator): T
}