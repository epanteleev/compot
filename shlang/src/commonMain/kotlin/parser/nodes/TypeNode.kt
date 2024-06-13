package parser.nodes

import types.*
import tokenizer.Identifier
import parser.nodes.visitors.TypeNodeVisitor
import tokenizer.CToken
import tokenizer.Keyword


abstract class AnyTypeNode : Node() {
    abstract fun name(): String
    abstract fun<T> accept(visitor: TypeNodeVisitor<T>): T
}

data class UnionSpecifier(val ident: Identifier, val fields: List<StructField>) : AnyTypeNode() {
    override fun<T> accept(visitor: TypeNodeVisitor<T>) = visitor.visit(this)
    override fun name(): String = ident.str()
}

data class UnionDeclaration(val name: Identifier) : AnyTypeNode() { //TODO separate class
    override fun<T> accept(visitor: TypeNodeVisitor<T>) = visitor.visit(this)
    override fun name(): String = name.str()
}

data class StructDeclaration(val name: Identifier) : AnyTypeNode() {
    override fun<T> accept(visitor: TypeNodeVisitor<T>) = visitor.visit(this)
    override fun name(): String = name.str()

    fun typeResolver(typeHolder: TypeHolder): BaseType {
        typeHolder.addStructType(name.str(), UncompletedStructType(name.str()))
        return UncompletedStructType(name.str())
    }
}

data class TypeQualifier(private val ident: Keyword): AnyTypeNode() {
    override fun name(): String = ident.str()

    override fun <T> accept(visitor: TypeNodeVisitor<T>): T {
        return visitor.visit(this)
    }

    fun qualifier(): PointerQualifier {
        return when (ident.str()) {
            "const"    -> PointerQualifier.CONST
            "volatile" -> PointerQualifier.VOLATILE
            "restrict" -> PointerQualifier.RESTRICT
            else       -> PointerQualifier.EMPTY
        }
    }
}

data class StorageClassSpecifier(private val ident: Keyword): AnyTypeNode() {
    override fun name(): String = ident.str()

    override fun <T> accept(visitor: TypeNodeVisitor<T>): T {
        return visitor.visit(this)
    }

    fun storageClass(): StorageClass {
        return when (ident.str()) {
            "typedef"  -> StorageClass.TYPEDEF
            "extern"   -> StorageClass.EXTERN
            "static"   -> StorageClass.STATIC
            "register" -> StorageClass.REGISTER
            "auto"     -> StorageClass.AUTO
            else       -> StorageClass.AUTO
        }
    }
}

data class TypeNode(val ident: CToken) : AnyTypeNode() {
    override fun<T> accept(visitor: TypeNodeVisitor<T>) = visitor.visit(this)
    override fun name(): String = ident.str()

    fun type(): BaseType {
        return when (ident.str()) {
            "void"    -> CPrimitive.VOID
            "char"    -> CPrimitive.CHAR
            "short"   -> CPrimitive.SHORT
            "int"     -> CPrimitive.INT
            "long"    -> CPrimitive.LONG
            "float"   -> CPrimitive.FLOAT
            "double"  -> CPrimitive.DOUBLE
            "signed"  -> CPrimitive.INT
            "unsigned"-> CPrimitive.UINT
            else      -> CPrimitive.UNKNOWN
        }
    }
}

data class StructSpecifier(val ident: Identifier, val fields: List<StructField>) : AnyTypeNode() {
    override fun<T> accept(visitor: TypeNodeVisitor<T>) = visitor.visit(this)
    override fun name(): String = ident.str()

    fun typeResolver(typeHolder: TypeHolder): StructBaseType {
        val structType = StructBaseType(ident.str())
        for (field in fields) {
            val type = field.declspec.resolveType(typeHolder)
            for (declarator in field.declarators) {
                structType.addField(declarator.name(), type)
            }
        }

        return structType
    }
}

data class EnumSpecifier(val ident: Identifier, val enumerators: List<Enumerator>) : AnyTypeNode() {
    override fun<T> accept(visitor: TypeNodeVisitor<T>) = visitor.visit(this)
    override fun name(): String = ident.str()
}

data class EnumDeclaration(val name: Identifier) : AnyTypeNode() {
    override fun<T> accept(visitor: TypeNodeVisitor<T>) = visitor.visit(this)
    override fun name(): String = name.str()
}