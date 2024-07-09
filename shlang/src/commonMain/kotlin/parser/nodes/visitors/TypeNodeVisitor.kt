package parser.nodes.visitors

import parser.nodes.*


interface TypeNodeVisitor<T> {
    fun visit(typeNode: TypeNode): T
    fun visit(structSpecifier: StructSpecifier): T
    fun visit(unionSpecifier: UnionSpecifier): T
    fun visit(enumSpecifier: EnumSpecifier): T
    fun visit(typeQualifier: TypeQualifierNode): T
    fun visit(storageClassSpecifier: StorageClassSpecifier): T
    fun visit(structField: StructField): T
    fun visit(structDeclaration: StructDeclaration): T
    fun visit(unionDeclaration: UnionDeclaration): T
    fun visit(enumDeclaration: EnumDeclaration): T
    fun visit(functionSpecifierNode: FunctionSpecifierNode): T
}