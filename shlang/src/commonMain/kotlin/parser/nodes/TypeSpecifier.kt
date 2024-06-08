package parser.nodes


import parser.nodes.visitors.Resolvable
import parser.nodes.visitors.TypeSpecifierVisitor

import types.CType
import types.CTypeBuilder
import types.TypeHolder


abstract class TypeSpecifier : Node() {
    abstract fun<T> accept(visitor: TypeSpecifierVisitor<T>): T
}

data class DeclarationSpecifier(val specifiers: List<AnyTypeNode>) : TypeSpecifier(), Resolvable {
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

    override fun<T> accept(visitor: TypeSpecifierVisitor<T>): T = visitor.visit(this)

    companion object {
        val EMPTY = DeclarationSpecifier(emptyList())
    }
}

data class TypeName(val specifiers: DeclarationSpecifier, val abstractDecl: AbstractDeclarator?) : TypeSpecifier(), Resolvable {
    override fun<T> accept(visitor: TypeSpecifierVisitor<T>): T = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        val specifierType = specifiers.resolveType(typeHolder)
        if (abstractDecl == null) {
            return specifierType
        }
        return abstractDecl.resolveType(specifierType, typeHolder)
    }
}