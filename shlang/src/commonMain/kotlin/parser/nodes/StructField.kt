package parser.nodes

import tokenizer.Position


class StructField(val declspec: DeclarationSpecifier, val declarators: List<StructDeclarator>) {
    fun begin(): Position = declspec.begin()
}