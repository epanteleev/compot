package parser.nodes

import tokenizer.*
import types.*


abstract class Node {
    abstract fun<T> accept(visitor: NodeVisitor<T>): T
}

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

data class FunctionPointerDeclarator(val declarator: List<Node>): AnyDeclarator() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    fun declarator(): Declarator {
        return declarator[0] as Declarator
    }

    override fun resolveType(typeHolder: TypeHolder): CType {
        TODO()
    }
}

data class DirectDeclarator(val decl: AnyDeclarator, val declarators: List<AnyDeclarator>): AnyDeclarator() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    fun name(): String {
        return when (decl) {
            is VarDeclarator             -> decl.name()
            is FunctionPointerDeclarator -> decl.declarator().name()
            else -> throw IllegalStateException("$decl")
        }
    }

    override fun resolveType(typeHolder: TypeHolder): CType {
        TODO()
    }
}

data class VarDeclarator(val ident: Ident) : AnyDeclarator() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    fun name(): String = ident.str()
    override fun resolveType(typeHolder: TypeHolder): CType {
        TODO()
    }
}

data class FunctionPointerParamDeclarator(val declarator: Node, val params: Node): AnyDeclarator() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        TODO()
    }
}

data class Declaration(val declspec: DeclarationSpecifier, val declarators: List<AnyDeclarator>): Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    fun resolveType(typeHolder: TypeHolder): List<String> {
        val type = declspec.resolveType(typeHolder)
        val vars = mutableListOf<String>()
        declarators.forEach {
            when (it) {
                is Declarator           -> {
                    it.resolveType(type, typeHolder)
                    vars.add(it.name())
                }
                is AssignmentDeclarator -> {
                    it.resolveType(type, typeHolder)
                    vars.add(it.name())
                }
                else -> TODO()
            }
        }
        return vars
    }
}

data class IdentNode(val str: Ident) : Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class NodePointer(val qualifiers: List<PointerQualifier>) : Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class Declspec(val type: CType, val ident: Ident) : Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class ArrayDeclarator(val constexpr: Expression) : AnyDeclarator() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        TODO()
    }
}

object EmptyDeclarator : AnyDeclarator() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType = CType.UNKNOWN
}

data class CompoundLiteral(val typeName: Node, val initializerList: Node) : Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class FunctionParams(val params: List<AnyParameter>): Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

abstract class AnyParameter : Node(), Resolvable

data class Parameter(val declspec: DeclarationSpecifier, val declarator: AnyDeclarator) : AnyParameter() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    fun name(): String {
        val varNode = declarator as Declarator
        return (varNode.directDeclarator.decl as VarDeclarator).ident.str()
    }

    override fun resolveType(typeHolder: TypeHolder): CType {
        val type = declspec.resolveType(typeHolder)
        return when (declarator) {
            is Declarator -> declarator.resolveType(type, typeHolder)
            is EmptyDeclarator -> type
            else -> throw IllegalStateException("Unknown declarator $declarator")
        }
    }
}

class ParameterVarArg: AnyParameter() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        return CType.UNKNOWN //TODO
    }
}

data class FunctionNode(val specifier: DeclarationSpecifier, val declarator: Declarator, val body: Statement) : Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    fun name(): String {
        val varNode = declarator.directDeclarator.decl as VarDeclarator
        return varNode.ident.str()
    }

    fun functionDeclarator(): FunctionDeclarator {
        return declarator.directDeclarator.declarators[0] as FunctionDeclarator
    }

    fun resolveType(typeHolder: TypeHolder): CFunctionType {
        val type = specifier.resolveType(typeHolder)
        val s = functionDeclarator().resolveParams(typeHolder)
        return CFunctionType(name(), type, s)
    }
}


abstract class AnyDeclarator: Node(), Resolvable

data class Declarator(val directDeclarator: DirectDeclarator, val pointers: List<NodePointer>): AnyDeclarator() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    fun name(): String {
        return directDeclarator.name()
    }

    override fun resolveType(typeHolder: TypeHolder): CType {
        TODO()
    }

    fun resolveType(baseType: CType, typeHolder: TypeHolder): CType {
        var pointerType = baseType
        for (pointer in pointers) {
            pointerType = CPointerType(pointerType)
        }

        if (directDeclarator.decl is FunctionPointerDeclarator) {
            val functionPointerDeclarator = directDeclarator.decl
            val fnDecl = directDeclarator.declarators[0] as FunctionDeclarator
            val params = fnDecl.resolveParams(typeHolder)
            pointerType = CFunPointerType(pointerType, params)

            typeHolder.add(functionPointerDeclarator.declarator().name(), pointerType)
        } else {
            for (decl in directDeclarator.declarators) {
                when (decl) {
                    is ArrayDeclarator -> {
                        val size = decl.constexpr as NumNode
                        pointerType = CompoundType(CArrayType(pointerType, size.toLong.data.toInt()))
                    }
                    else -> throw IllegalStateException("Unknown declarator $decl")
                }
            }

            typeHolder.add(name(), pointerType)
        }


        return pointerType
    }
}

data class AssignmentDeclarator(val rvalue: Declarator, val lvalue: Expression): AnyDeclarator() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        TODO("Not yet implemented")
    }

    fun name(): String {
        return rvalue.name()
    }

    fun resolveType(ctype: CType, typeHolder: TypeHolder): CType {
        return rvalue.resolveType(ctype, typeHolder)
    }
}

data class FunctionDeclarator(val params: List<AnyParameter>): AnyDeclarator() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        params.forEach { it.resolveType(typeHolder) }
        return CType.UNKNOWN
    }

    fun params(): List<String> {
        return params.map {
            when (it) {
                is Parameter -> it.name()
                is ParameterVarArg -> "..."
                else -> throw IllegalStateException("Unknown parameter $it")

            }
        }
    }

    fun resolveParams(typeHolder: TypeHolder): List<CType> {
        return params.map { it.resolveType(typeHolder) }
    }
}

data class ProgramNode(val nodes: MutableList<Node>) : Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class AbstractDeclarator(val pointers: List<NodePointer>, val directAbstractDeclarator: List<Node>) : AnyDeclarator() {   //TODO
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    override fun resolveType(typeHolder: TypeHolder): CType {
        TODO()
    }
}

data class DirectFunctionDeclarator(val parameters: List<AnyParameter>) : Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class DirectArrayDeclarator(val size: Node) : Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

data class StructField(val declspec: DeclarationSpecifier, val declarators: List<StructDeclarator>): Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)

    fun resolveType(typeHolder: TypeHolder): CType {
        TODO()
    }
}

class Enumerator(val ident: Ident, val expr: Node) : Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}

abstract class AnyTypeNode : Node() {
    abstract fun name(): String
}

data class TypeNode(val ident: Ident) : AnyTypeNode() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
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

data class TypeQualifier(private val ident: Ident): AnyTypeNode() {
    override fun name(): String = ident.str()

    override fun <T> accept(visitor: NodeVisitor<T>): T {
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

data class StorageClassSpecifier(private val ident: Ident): AnyTypeNode() {
    override fun name(): String = ident.str()

    override fun <T> accept(visitor: NodeVisitor<T>): T {
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

data class UnionSpecifier(val ident: Ident, val fields: List<StructField>) : AnyTypeNode() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
    override fun name(): String = ident.str()
}

data class UnionDeclaration(val name: Ident) : AnyTypeNode() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
    override fun name(): String = name.str()
}

data class StructDeclaration(val name: Ident) : AnyTypeNode() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
    override fun name(): String = name.str()

    fun typeResolver(typeHolder: TypeHolder): BaseType {
        typeHolder.addStructType(name.str(), UncompletedStructType(name.str()))
        return UncompletedStructType(name.str())
    }
}

data class StructSpecifier(val ident: Ident, val fields: List<StructField>) : AnyTypeNode() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
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

data class EnumSpecifier(val ident: Ident, val enumerators: List<Node>) : AnyTypeNode() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
    override fun name(): String = ident.str()
}

data class EnumDeclaration(val name: Ident) : AnyTypeNode() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
    override fun name(): String = name.str()
}

data class StructDeclarator(val declarator: AnyDeclarator, val expr: Expression): Node() {
    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visit(this)
    }

    fun name(): String {
        return when (declarator) {
            is Declarator -> declarator.name()
            else -> throw IllegalStateException("$declarator")
        }
    }

    fun resolveType(typeHolder: TypeHolder): CType {
        TODO()
    }
}

object DummyNode : Node() {
    override fun<T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}