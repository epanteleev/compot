package parser.nodes

import typedesc.*
import tokenizer.tokens.*
import tokenizer.tokens.CToken
import parser.nodes.visitors.TypeNodeVisitor
import tokenizer.Position


sealed class AnyTypeNode(val name: CToken) {
    fun begin(): Position = name.position()
    fun name(): String = name.str()

    abstract fun<T> accept(visitor: TypeNodeVisitor<T>): T
}

class UnionSpecifier(name: Identifier, val fields: List<StructField>) : AnyTypeNode(name) {
    override fun<T> accept(visitor: TypeNodeVisitor<T>) = visitor.visit(this)
}

class UnionDeclaration(name: Identifier) : AnyTypeNode(name) {
    override fun<T> accept(visitor: TypeNodeVisitor<T>) = visitor.visit(this)
}

class TypeQualifierNode(name: Keyword): AnyTypeNode(name) {
    override fun toString(): String = name.str()
    override fun <T> accept(visitor: TypeNodeVisitor<T>): T {
        return visitor.visit(this)
    }

    fun qualifier(): TypeQualifier = when (name.str()) {
        "const"    -> TypeQualifier.CONST
        "volatile" -> TypeQualifier.VOLATILE
        "restrict" -> TypeQualifier.RESTRICT
        else       -> throw IllegalStateException("Unknown type qualifier '$name'")
    }
}

class StorageClassSpecifier(name: Keyword): AnyTypeNode(name) {
    override fun toString(): String = name.str()
    override fun <T> accept(visitor: TypeNodeVisitor<T>): T {
        return visitor.visit(this)
    }

    fun storageClass(): StorageClass = when (name.str()) {
        "typedef"  -> StorageClass.TYPEDEF
        "extern"   -> StorageClass.EXTERN
        "static"   -> StorageClass.STATIC
        "register" -> StorageClass.REGISTER
        "auto"     -> StorageClass.AUTO
        else       -> throw IllegalStateException("Unknown storage class $name")
    }
}

class TypeNode(name: CToken) : AnyTypeNode(name) {
    override fun toString(): String = name.str()
    override fun<T> accept(visitor: TypeNodeVisitor<T>) = visitor.visit(this)
}

class StructSpecifier(name: Identifier, val fields: List<StructField>) : AnyTypeNode(name) {
    override fun<T> accept(visitor: TypeNodeVisitor<T>) = visitor.visit(this)
}

class StructDeclaration(name: Identifier) : AnyTypeNode(name) {
    override fun<T> accept(visitor: TypeNodeVisitor<T>) = visitor.visit(this)
}

class Enumerator(private val ident: Identifier, val constExpr: Expression) {
    fun begin(): Position = ident.position()
    fun name() = ident.str()
}

class EnumSpecifier(name: Identifier, val enumerators: List<Enumerator>) : AnyTypeNode(name) {
    override fun<T> accept(visitor: TypeNodeVisitor<T>) = visitor.visit(this)
}

class EnumDeclaration(name: Identifier) : AnyTypeNode(name) {
    override fun<T> accept(visitor: TypeNodeVisitor<T>) = visitor.visit(this)
}

// 6.7.4 Function specifiers
// https://port70.net/~nsz/c/c11/n1570.html#6.7.4
class FunctionSpecifierNode(name: Keyword) : AnyTypeNode(name) {
    override fun <T> accept(visitor: TypeNodeVisitor<T>): T {
        return visitor.visit(this)
    }

    fun specifier(): TypeProperty = when (name.str()) {
        "inline"   -> FunctionSpecifier.INLINE
        "noreturn" -> FunctionSpecifier.NORETURN
        else -> throw IllegalStateException("Unknown function specifier $name")
    }
}