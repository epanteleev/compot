package parser.nodes

import tokenizer.Position


data class DirectDeclarator(val decl: DirectDeclaratorEntry, val directDeclaratorParams: List<DirectDeclaratorParam>) {
    fun begin(): Position = decl.begin()

    fun parameterTypeList(): ParameterTypeList {
        return directDeclaratorParams[0] as ParameterTypeList
    }

    fun name(): String = decl.name()
}