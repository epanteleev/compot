package parser.nodes.visitors

import parser.nodes.*


interface ParameterVisitor<T> {
    fun visit(parameter: Parameter): T
    fun visit(parameterVarArg: ParameterVarArg): T
}