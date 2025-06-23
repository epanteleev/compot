package sema

import codegen.consteval.*
import common.assertion
import intrinsic.VaStart
import parser.nodes.*
import parser.nodes.visitors.TypeNodeVisitor
import tokenizer.Position
import typedesc.*
import types.*


internal class CTypeBuilder internal constructor(private val where: Position, private val sema: SemanticAnalysis): TypeNodeVisitor<TypeProperty> {
    private val typeProperties = mutableListOf<TypeQualifier>()
    private val baseTypes = mutableListOf<CType>()
    private var storageClass: StorageClass? = null

    fun add(property: TypeProperty) {
        when (property) {
            is CType -> baseTypes.add(property)
            is StorageClass -> {
                assertion(storageClass == null) {
                    "Multiple storage classes are not allowed: $storageClass, $property"
                }
                storageClass = property
            }

            is TypeQualifier -> typeProperties.add(property)
            is FunctionSpecifier -> {
                // skip
            }
            else -> throw IllegalArgumentException("Unknown property: $property")
        }
    }

    private fun check(baseTypes: List<CType>, vararg types: CPrimitive): Boolean {
        if (baseTypes.size != types.size) {
            return false
        }
        types.forEachIndexed { index, type ->
            if (baseTypes[index] != type) {
                return false
            }
        }
        return true
    }

    private fun foldCTypes(baseTypes: List<CType>): CompletedType {
        when {
            check(baseTypes, UINT, LONG, LONG, INT) -> return ULONG
            check(baseTypes, UINT, LONG, LONG)      -> return ULONG
            check(baseTypes, UINT, SHORT, INT)      -> return USHORT
            check(baseTypes, UINT, LONG, INT)       -> return ULONG
            check(baseTypes, INT, SHORT, INT)       -> return SHORT
            check(baseTypes, INT, LONG, LONG)       -> return LONG
            check(baseTypes, INT, LONG, INT)        -> return LONG
            check(baseTypes, LONG, LONG, INT)       -> return LONG
            check(baseTypes, LONG, UINT, INT)       -> return ULONG
            check(baseTypes, INT, INT)              -> return INT
            check(baseTypes, UINT, CHAR)            -> return UCHAR
            check(baseTypes, UINT, SHORT)           -> return USHORT
            check(baseTypes, UINT, INT)             -> return UINT
            check(baseTypes, UINT, LONG)            -> return ULONG
            check(baseTypes, LONG, LONG)            -> return LONG
            check(baseTypes, INT, CHAR)             -> return CHAR
            check(baseTypes, INT, SHORT)            -> return SHORT
            check(baseTypes, SHORT, INT)            -> return SHORT
            check(baseTypes, LONG, INT)             -> return LONG
            check(baseTypes, USHORT, INT)           -> return USHORT
            check(baseTypes, LONG, DOUBLE)          -> {
                // TODO println("Warning: long double is not supported, using double instead in $position")
                return DOUBLE
            }
        }
        assertion(baseTypes.size == 1) {
            "Unknown type '$baseTypes'"
        }
        return baseTypes[0].asType(where)
    }

    fun build(specifiers: List<AnyTypeNode>): DeclSpec {
        for (specifier in specifiers) {
            add(specifier.accept(this))
        }

        val baseType = if (baseTypes[0] is CPrimitive) {
            foldCTypes(baseTypes)
        } else {
            baseTypes[0]
        }

        val cType = TypeDesc.Companion.from(baseType, typeProperties)
        return DeclSpec(cType, storageClass)
    }

    private fun resolveFieldTypes(fields: List<StructField>): List<Member> {
        val members = arrayListOf<Member>()
        for (field in fields) {
            if (field.declarators.isEmpty()) {
                val type = field.declspec.accept(sema)
                members.add(AnonMember(type.typeDesc))
                continue
            }

            for (declarator in field.declarators) {
                val declSpec = field.declspec.accept(sema)
                val resolved = declareStructType(declarator, declSpec)
                members.add(FieldMember(declarator.name(), resolved.toTypeDesc()))
            }
        }

        return members
    }

    private fun declareStructType(structDeclarator: StructDeclarator, declSpec: DeclSpec): VarDescriptor {
        if (structDeclarator.expr !is EmptyExpression) {
            println("Warning: bit field is not supported")
        }

        return declareStructDeclaratorItem(structDeclarator.declarator, declSpec)
    }

    private fun declareStructDeclaratorItem(anyStructDeclaratorItem: AnyStructDeclaratorItem, declSpec: DeclSpec) = when (anyStructDeclaratorItem) {
        is StructDeclaratorItem -> sema.declareVar(anyStructDeclaratorItem.expr, declSpec)
            ?: throw IllegalStateException("Typedef is not supported in struct fields")
        is EmptyStructDeclaratorItem -> {
            VarDescriptor(anyStructDeclaratorItem.name(), declSpec.typeDesc.asType(anyStructDeclaratorItem.begin()), declSpec.typeDesc.qualifiers(), declSpec.storageClass)
        }
    }

    override fun visit(typeNode: TypeNode): TypeProperty = when (val name = typeNode.name()) {
        "void"    -> VOID
        "char"    -> CHAR
        "short"   -> SHORT
        "int"     -> INT
        "long"    -> LONG
        "float"   -> FLOAT
        "double"  -> DOUBLE
        "signed"  -> INT
        "unsigned"-> UINT
        "_Bool"   -> BOOL
        "__builtin_va_list" -> VaStart.vaList
        else      -> sema.typeHolder.getTypedef(name).cType()
    }

    override fun visit(structSpecifier: StructSpecifier): TypeProperty {
        val structType = CStructType.create(structSpecifier.name(), resolveFieldTypes(structSpecifier.fields))
        return sema.typeHolder.addNewType(structSpecifier.name(), structType)
    }

    override fun visit(unionSpecifier: UnionSpecifier): TypeProperty {
        val structType = CUnionType.create(unionSpecifier.name(), resolveFieldTypes(unionSpecifier.fields))
        return sema.typeHolder.addNewType(unionSpecifier.name(), structType)
    }

    private fun constEval(expr: Expression, enumeratorValues: Map<String, Int>): Int {
        val ctx = CommonConstEvalContext<Int>(sema, enumeratorValues)
        val constExpr = ConstEvalExpression.eval(expr, TryConstEvalExpressionInt(ctx))
            ?: throw IllegalStateException("Cannot evaluate enum value")

        return constExpr
    }

    override fun visit(enumSpecifier: EnumSpecifier): TypeProperty {
        val enumeratorValues = hashMapOf<String, Int>()
        var enumValue = 0
        for (field in enumSpecifier.enumerators) {
            val constExpression = field.constExpr
            if (constExpression !is EmptyExpression) {
                enumValue = constEval(constExpression, enumeratorValues)
            }
            enumeratorValues[field.name()] = enumValue
            enumValue++
        }

        return sema.typeHolder.addNewType(enumSpecifier.name(), CEnumType(enumSpecifier.name(), enumeratorValues))
    }

    override fun visit(typeQualifier: TypeQualifierNode): TypeProperty = typeQualifier.qualifier()

    override fun visit(storageClassSpecifier: StorageClassSpecifier): StorageClass = storageClassSpecifier.storageClass()

    override fun visit(structDeclaration: StructDeclaration): TypeProperty {
        return sema.typeHolder.getStructTypeOrNull(structDeclaration.name()) ?:
            sema.typeHolder.addNewType(structDeclaration.name(), CUncompletedStructType(structDeclaration.name()))
    }

    override fun visit(unionDeclaration: UnionDeclaration): TypeProperty {
        return sema.typeHolder.getUnionTypeOrNull(unionDeclaration.name()) ?:
            sema.typeHolder.addNewType(unionDeclaration.name(), CUncompletedUnionType(unionDeclaration.name()))
    }

    override fun visit(enumDeclaration: EnumDeclaration): TypeProperty {
        return sema.typeHolder.getEnumTypeOrNull(enumDeclaration.name()) ?: CUncompletedEnumType(enumDeclaration.name())
    }

    override fun visit(functionSpecifierNode: FunctionSpecifierNode): TypeProperty = functionSpecifierNode.specifier()
}