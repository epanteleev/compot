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

    fun typeResolver(typeHolder: TypeHolder): BaseType {
        val structType = UnionBaseType(name())
        for (field in fields) {
            val type = field.declspec.resolveType(typeHolder)
            for (declarator in field.declarators) {
                structType.addField(declarator.name(), type)
            }
        }

        typeHolder.addStructType(ident.str(), structType)
        return structType
    }

    override fun name(): String = ident.str()
}

data class UnionDeclaration(val name: Identifier) : AnyTypeNode() { //TODO separate class
    override fun<T> accept(visitor: TypeNodeVisitor<T>) = visitor.visit(this)

    fun typeResolver(typeHolder: TypeHolder): BaseType {
        typeHolder.addStructType(name(), UncompletedUnionType(name()))
        return UncompletedStructType(name())
    }

    override fun name(): String = name.str()
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

    fun resolveType(typeHolder: TypeHolder): BaseType {
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
            else      -> {
                return typeHolder.getStructType(ident.str())
            }
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
                val resolved = declarator.resolveType(type, typeHolder)
                structType.addField(declarator.name(), resolved)
            }
        }

        typeHolder.addStructType(ident.str(), structType)
        return structType
    }
}

data class StructDeclaration(val name: Identifier) : AnyTypeNode() {
    override fun<T> accept(visitor: TypeNodeVisitor<T>) = visitor.visit(this)
    override fun name(): String = name.str()

    fun typeResolver(typeHolder: TypeHolder): BaseType {
        typeHolder.addStructType(name.str(), UncompletedStructType(name.str()))
        return UncompletedStructType(name.str())
    }
}

data class EnumSpecifier(val ident: Identifier, val enumerators: List<Enumerator>) : AnyTypeNode() {
    override fun<T> accept(visitor: TypeNodeVisitor<T>) = visitor.visit(this)

    fun typeResolver(typeHolder: TypeHolder): EnumBaseType {
        val enumBaseType = EnumBaseType(name())
        for (field in enumerators) {
            enumBaseType.addEnumeration(field.name())
        }

        typeHolder.addStructType(name(), enumBaseType)
        return enumBaseType
    }

    override fun name(): String = ident.str()
}

data class EnumDeclaration(val name: Identifier) : AnyTypeNode() {
    override fun<T> accept(visitor: TypeNodeVisitor<T>) = visitor.visit(this)

    fun typeResolver(typeHolder: TypeHolder): BaseType {
        typeHolder.addStructType(name(), UncompletedEnumType(name()))
        return UncompletedStructType(name())
    }

    override fun name(): String = name.str()
}