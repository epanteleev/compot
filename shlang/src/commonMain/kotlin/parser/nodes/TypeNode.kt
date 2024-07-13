package parser.nodes

import types.*
import tokenizer.Identifier
import parser.nodes.visitors.TypeNodeVisitor
import tokenizer.CToken
import tokenizer.Keyword


abstract class AnyTypeNode : Node() {
    abstract fun name(): String
    abstract fun<T> accept(visitor: TypeNodeVisitor<T>): T

    abstract fun typeResolve(typeHolder: TypeHolder, typeBuilder: CTypeBuilder): TypeProperty

    protected fun addToBuilder(typeBuilder: CTypeBuilder, closure: () -> TypeProperty): TypeProperty {
        val property = closure()
        typeBuilder.add(property)
        return property
    }
}

data class UnionSpecifier(val name: Identifier, val fields: List<StructField>) : AnyTypeNode() {
    override fun<T> accept(visitor: TypeNodeVisitor<T>) = visitor.visit(this)

    override fun typeResolve(typeHolder: TypeHolder, typeBuilder: CTypeBuilder) = addToBuilder(typeBuilder) {
        val structType = UnionBaseType(name())
        for (field in fields) {
            val type = field.declspec.specifyType(typeHolder)
            for (declarator in field.declarators) {
                structType.addField(declarator.name(), type)
            }
        }

        name.let { typeHolder.addNewType(it.str(), structType) }
        return@addToBuilder structType
    }

    override fun name(): String {
        return name.str()
    }
}

data class UnionDeclaration(val name: Identifier) : AnyTypeNode() { //TODO separate class
    override fun<T> accept(visitor: TypeNodeVisitor<T>) = visitor.visit(this)

    override fun typeResolve(typeHolder: TypeHolder, typeBuilder: CTypeBuilder) = addToBuilder(typeBuilder) {
        typeHolder.getTypeOrNull(name.str()) ?: typeHolder.addNewType(name.str(), UncompletedUnionBaseType(name.str()))
    }

    override fun name(): String = name.str()
}

data class TypeQualifierNode(private val name: Keyword): AnyTypeNode() {
    override fun name(): String = name.str()

    override fun <T> accept(visitor: TypeNodeVisitor<T>): T {
        return visitor.visit(this)
    }

    override fun typeResolve(typeHolder: TypeHolder, typeBuilder: CTypeBuilder) = addToBuilder(typeBuilder) {
        qualifier()
    }

    fun qualifier(): TypeQualifier {
        return when (name.str()) {
            "const"    -> TypeQualifier.CONST
            "volatile" -> TypeQualifier.VOLATILE
            "restrict" -> TypeQualifier.RESTRICT
            else       -> TypeQualifier.EMPTY
        }
    }
}

data class StorageClassSpecifier(private val name: Keyword): AnyTypeNode() {
    override fun name(): String = name.str()

    override fun <T> accept(visitor: TypeNodeVisitor<T>): T {
        return visitor.visit(this)
    }

    override fun typeResolve(typeHolder: TypeHolder, typeBuilder: CTypeBuilder): TypeProperty {
        val storageClass = storageClass()
        if (storageClass != StorageClass.TYPEDEF) {
           typeBuilder.add(storageClass)
        }

        return storageClass
    }

    fun storageClass(): StorageClass {
        return when (name.str()) {
            "typedef"  -> StorageClass.TYPEDEF
            "extern"   -> StorageClass.EXTERN
            "static"   -> StorageClass.STATIC
            "register" -> StorageClass.REGISTER
            "auto"     -> StorageClass.AUTO
            else       -> throw IllegalStateException("Unknown storage class $name")
        }
    }
}

data class TypeNode(private val name: CToken) : AnyTypeNode() {
    override fun<T> accept(visitor: TypeNodeVisitor<T>) = visitor.visit(this)
    override fun name(): String = name.str()

    override fun typeResolve(typeHolder: TypeHolder, typeBuilder: CTypeBuilder) = addToBuilder(typeBuilder) {
        when (name.str()) {
            "void"    -> CPrimitive.VOID
            "char"    -> CPrimitive.CHAR
            "short"   -> CPrimitive.SHORT
            "int"     -> CPrimitive.INT
            "long"    -> CPrimitive.LONG
            "float"   -> CPrimitive.FLOAT
            "double"  -> CPrimitive.DOUBLE
            "signed"  -> CPrimitive.INT
            "unsigned"-> CPrimitive.UINT
            "__builtin_va_list" -> CPrimitive.LONG //TODO hack
            else      -> typeHolder.getStructType(name.str())
        }
    }
}

data class StructSpecifier(private val name: Identifier, val fields: List<StructField>) : AnyTypeNode() {
    override fun<T> accept(visitor: TypeNodeVisitor<T>) = visitor.visit(this)
    override fun name(): String = name.str()

    override fun typeResolve(typeHolder: TypeHolder, typeBuilder: CTypeBuilder) = addToBuilder(typeBuilder) {
        val structType = StructBaseType(name.str())
        for (field in fields) {
            val type = field.declspec.specifyType(typeHolder)
            for (declarator in field.declarators) {
                val resolved = declarator.resolveType(field.declspec, typeHolder)
                structType.addField(declarator.name(), resolved)
            }
        }

        typeHolder.addNewType(name.str(), structType)
        return@addToBuilder structType
    }
}

data class StructDeclaration(private val name: Identifier) : AnyTypeNode() {
    override fun<T> accept(visitor: TypeNodeVisitor<T>) = visitor.visit(this)
    override fun name(): String = name.str()

    override fun typeResolve(typeHolder: TypeHolder, typeBuilder: CTypeBuilder) = addToBuilder(typeBuilder) {
        typeHolder.getTypeOrNull(name.str()) ?: typeHolder.addNewType(name.str(), UncompletedStructBaseType(name.str()))
    }
}

data class EnumSpecifier(private val name: Identifier, val enumerators: List<Enumerator>) : AnyTypeNode() {
    override fun<T> accept(visitor: TypeNodeVisitor<T>) = visitor.visit(this)

    override fun typeResolve(typeHolder: TypeHolder, typeBuilder: CTypeBuilder) = addToBuilder(typeBuilder) {
        val enumBaseType = EnumBaseType(name.str())
        for (field in enumerators) {
            enumBaseType.addEnumeration(field.name())
        }

        return@addToBuilder enumBaseType
    }

    override fun name(): String = name.str()
}

data class EnumDeclaration(private val name: Identifier) : AnyTypeNode() {
    override fun<T> accept(visitor: TypeNodeVisitor<T>) = visitor.visit(this)

    override fun typeResolve(typeHolder: TypeHolder, typeBuilder: CTypeBuilder) = addToBuilder(typeBuilder) {
        typeHolder.getTypeOrNull(name.str()) ?: UncompletedEnumType(name.str())
    }

    override fun name(): String = name.str()
}

// 6.7.4 Function specifiers
// https://port70.net/~nsz/c/c11/n1570.html#6.7.4
data class FunctionSpecifierNode(private val name: Keyword) : AnyTypeNode() {
    override fun name(): String = name.str()

    override fun <T> accept(visitor: TypeNodeVisitor<T>): T {
        return visitor.visit(this)
    }

    override fun typeResolve(typeHolder: TypeHolder, typeBuilder: CTypeBuilder): TypeProperty {
        return when (name.str()) {
            "inline"   -> FunctionSpecifier.INLINE
            "noreturn" -> FunctionSpecifier.NORETURN
            else -> throw IllegalStateException("Unknown function specifier $name")
        }
    }
}