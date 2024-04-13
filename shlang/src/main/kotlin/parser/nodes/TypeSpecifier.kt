package parser.nodes

import types.CType
import types.CTypeBuilder
import types.TypeHolder

abstract class TypeSpecifier : Node()

data class DeclarationSpecifier(val specifiers: List<AnyTypeNode>) : TypeSpecifier(), Resolvable {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        val ctypeBuilder = CTypeBuilder()
        specifiers.forEach {
            when (it) {
                is TypeNode -> {
                    ctypeBuilder.add(it.type()) //TODO
                }
                is TypeQualifier -> {
                    ctypeBuilder.add(it.qualifier())
                }
                is StorageClassSpecifier -> {
                    ctypeBuilder.add(it.storageClass())
                }
                is StructSpecifier -> {
                    ctypeBuilder.add(it.typeResolver(typeHolder))
                }
                is UnionSpecifier -> {
                    TODO()
                }
                is EnumSpecifier -> {
                    TODO()
                }
                is EnumDeclaration -> {
                    TODO()
                }
                is StructDeclaration -> {
                    ctypeBuilder.add(it.typeResolver(typeHolder))
                }
            }
        }

        return ctypeBuilder.build(typeHolder)
    }

    companion object {
        val EMPTY = DeclarationSpecifier(emptyList())
    }
}

data class TypeName(val specifiers: DeclarationSpecifier, val abstractDecl: Node) : TypeSpecifier(), Resolvable {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        TODO("Not yet implemented")
    }
}