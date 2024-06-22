package parser.nodes

import parser.nodes.visitors.TypeSpecifierVisitor
import types.*


abstract class TypeSpecifier : Node() {
    abstract fun<T> accept(visitor: TypeSpecifierVisitor<T>): T
    abstract fun resolveType(typeHolder: TypeHolder): CType
}

data class DeclarationSpecifier(val specifiers: List<AnyTypeNode>) : TypeSpecifier() {
    var isTypedef = false

    override fun resolveType(typeHolder: TypeHolder): CType {
        val ctypeBuilder = CTypeBuilder()
        specifiers.forEach {
            when (it) {
                is TypeNode -> {
                    ctypeBuilder.add(it.resolveType(typeHolder)) //TODO
                }
                is TypeQualifier -> {
                    ctypeBuilder.add(it.qualifier())
                }
                is StorageClassSpecifier -> {
                    if (it.storageClass() == StorageClass.TYPEDEF) {
                        isTypedef = true
                    } else {
                        ctypeBuilder.add(it.storageClass())
                    }
                }
                is StructSpecifier -> {
                    ctypeBuilder.add(it.typeResolver(typeHolder))
                }
                is UnionSpecifier -> {
                    ctypeBuilder.add(it.typeResolver(typeHolder))
                }
                is EnumSpecifier -> {
                    ctypeBuilder.add(it.typeResolver(typeHolder))
                }
                is EnumDeclaration -> {
                    ctypeBuilder.add(it.typeResolver(typeHolder))
                }
                is StructDeclaration -> {
                    ctypeBuilder.add(it.typeResolver(typeHolder))
                }
                is UnionDeclaration -> {
                    ctypeBuilder.add(it.typeResolver(typeHolder))
                }
                else -> {
                    TODO()
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

data class TypeName(val specifiers: DeclarationSpecifier, val abstractDecl: AbstractDeclarator?) : TypeSpecifier() {
    override fun<T> accept(visitor: TypeSpecifierVisitor<T>): T = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        val specifierType = specifiers.resolveType(typeHolder)
        if (abstractDecl == null) {
            return specifierType
        }
        return abstractDecl.resolveType(specifierType, typeHolder)
    }
}