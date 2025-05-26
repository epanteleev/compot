package sema

import codegen.consteval.*
import parser.LineAgnosticAstPrinter
import parser.nodes.*
import parser.nodes.BinaryOpType
import parser.nodes.BinaryOpType.AND
import parser.nodes.BinaryOpType.COMMA
import parser.nodes.BinaryOpType.OR

import parser.nodes.visitors.ExpressionVisitor
import parser.nodes.visitors.TypeSpecifierVisitor
import typedesc.*
import types.*


class SemanticAnalysis internal constructor(val typeHolder: TypeHolder): ExpressionVisitor<CompletedType>, TypeSpecifierVisitor<DeclSpec> {
    private val cachedExpressionTypes = hashMapOf<Expression, CompletedType>()
    private val cachedTypeSpecs = hashMapOf<TypeSpecifier, DeclSpec>()

    private inline fun<reified T: CompletedType> memoize(expression: Expression, closure: () -> T): T {
        val cachedType = cachedExpressionTypes[expression]
        if (cachedType != null) {
            return cachedType as T
        }

        val type = closure()
        cachedExpressionTypes[expression] = type
        return type
    }

    private inline fun memoizeType(typeSpecifier: TypeSpecifier, closure: () -> DeclSpec): DeclSpec {
        val cachedType = cachedTypeSpecs[typeSpecifier]
        if (cachedType != null) {
            return cachedType
        }

        val type = closure()
        cachedTypeSpecs[typeSpecifier] = type
        return type
    }

    private fun wrapPointers(type: CType, pointers: List<NodePointer>): CType {
        var pointerType = type
        for (pointer in pointers) {
            pointerType = CPointer(pointerType, pointer.property())
        }
        return pointerType
    }

    fun resolveTypedef(declarator: Declarator, declSpec: DeclSpec): Typedef? {
        if (declSpec.storageClass != StorageClass.TYPEDEF) {
            return null
        }

        val pointerType = wrapPointers(declSpec.typeDesc.cType(), declarator.pointers)
        val newTypeDesc = TypeDesc.from(pointerType, declSpec.typeDesc.qualifiers())

        val type = resolveDirectDeclarator(declarator.directDeclarator, newTypeDesc)
        return Typedef(declarator.name(), type)
    }

    fun declareVar(declarator: AnyDeclarator, declSpec: DeclSpec): VarDescriptor? = when (declarator) {
        is Declarator -> declareVar(declarator, declSpec)
        is InitDeclarator -> declareVar(declarator, declSpec)
    }

    private fun declareVar(declarator: Declarator, declSpec: DeclSpec): VarDescriptor? {
        if (declSpec.storageClass == StorageClass.TYPEDEF) {
            return null
        }

        val pointerType = wrapPointers(declSpec.typeDesc.cType(), declarator.pointers)
        val newTypeDesc = TypeDesc.from(pointerType, declSpec.typeDesc.qualifiers())
        val type = resolveDirectDeclarator(declarator.directDeclarator, newTypeDesc)
        return VarDescriptor(declarator.name(), type.asType(declarator.begin()), type.qualifiers(), declSpec.storageClass)
    }

    private fun declareVar(initDeclarator: InitDeclarator, declSpec: DeclSpec): VarDescriptor {
        val pointerType = wrapPointers(declSpec.typeDesc.cType(), initDeclarator.declarator.pointers)
        val newTypeDesc = TypeDesc.from(pointerType, declSpec.typeDesc.qualifiers())

        val type = resolveDirectDeclarator(initDeclarator.declarator.directDeclarator, newTypeDesc)
        val baseType = type.cType()
        if (baseType !is CUncompletedArrayType) {
            return VarDescriptor(initDeclarator.name(), baseType.asType(initDeclarator.begin()), type.qualifiers(), declSpec.storageClass)
        }

        when (initDeclarator.rvalue) {
            is InitializerListInitializer -> {
                // Special case for array initialization without exact size like:
                // int a[] = {1, 2};
                // 'a' is array of 2 elements, not pointer to int

                val initializerList = initDeclarator.rvalue.list
                when (val initializerType = resolveInitializerList(initializerList)) {
                    is InitializerType -> {
                        val rvalueType = CArrayType(baseType.element(), initializerList.length().toLong())
                        return VarDescriptor(initDeclarator.name(), rvalueType, listOf(), declSpec.storageClass)
                    }
                    is CStringLiteral -> {
                        val rvalueType = CArrayType(baseType.element(), initializerType.dimension + 1)
                        return VarDescriptor(initDeclarator.name(), rvalueType, listOf(), declSpec.storageClass)
                    }
                    else -> throw TypeResolutionException("Array size is not specified: type=$initializerType", initDeclarator.declarator.begin())
                }
            }
            is ExpressionInitializer -> {
                val expr = initDeclarator.rvalue.expr
                if (expr !is StringNode) {
                    throw TypeResolutionException("Array size is not specified", initDeclarator.declarator.begin())
                }
                // Special case for string initialization like:
                // char a[] = "hello";
                return VarDescriptor(initDeclarator.name(), expr.accept(this), listOf(), declSpec.storageClass)
            }
        }
    }

    private fun resolveAllDeclaratorsInDirectDeclarator(directDeclarator: DirectDeclarator, baseType: TypeDesc): TypeDesc {
        var currentType = baseType
        for (directDeclaratorParam in directDeclarator.directDeclaratorParams.reversed()) {
            currentType = when (directDeclaratorParam) {
                is ArrayDeclarator -> {
                    resolveArrayDeclaratorType(directDeclaratorParam, currentType)
                }
                is ParameterTypeList -> {
                    resolveParameterTypeList(directDeclaratorParam, currentType)
                }
                is IdentifierList -> throw IllegalStateException("Identifier list is not supported")
            }
        }
        return currentType
    }

    private fun resolveDirectDeclarator(directDeclarator: DirectDeclarator, baseType: TypeDesc): TypeDesc {
        val resolvedDeclList = resolveAllDeclaratorsInDirectDeclarator(directDeclarator, baseType)
        return when (val decl = directDeclarator.decl) {
            is FunctionDeclarator ->  resolveFunctionDeclarator(decl, resolvedDeclList)
            is DirectVarDeclarator -> resolvedDeclList
        }
    }

    private fun resolveFunctionDeclarator(functionDeclarator: FunctionDeclarator, resolvedDeclList: TypeDesc): TypeDesc {
        val ctype = wrapPointers(resolvedDeclList.cType(), functionDeclarator.declarator.pointers)
        val typeDesc = TypeDesc.from(ctype, resolvedDeclList.qualifiers())
        return resolveDirectDeclarator(functionDeclarator.declarator.directDeclarator, typeDesc)
    }

    fun resolveParameterList(parameterTypeList: ParameterTypeList): List<VarDescriptor>? {
        if (parameterTypeList.params.isEmpty()) {
            return emptyList()
        }
        val varDescs = arrayListOf<VarDescriptor>()
        for (param in parameterTypeList.params) {
            if (param !is Parameter) {
                continue
            }

            val varDesc = resolveParameterVarDesc(param) ?: return null
            varDescs.add(varDesc)
        }

        return varDescs
    }

    private fun resolveParameterVarDesc(parameter: Parameter): VarDescriptor? {
        val type = resolveParameterType( parameter)
        val name = parameter.name() ?: return null
        return VarDescriptor(name, type.asType(parameter.begin()), type.qualifiers(), null)
    }

    private fun resolveParameterDeclaratorType(parameter: AnyParamDeclarator, declSpec: DeclSpec): TypeDesc = when (parameter) {
        is ParamAbstractDeclarator -> resolveAbstractDeclaratorType(parameter.abstractDeclarator, declSpec.typeDesc)
        is ParamDeclarator -> {
            val varDesc = declareVar(parameter.declarator, declSpec)
                ?: throw IllegalStateException("Typedef is not supported in function parameters")

            varDesc.toTypeDesc()
        }
        is EmptyParamDeclarator -> declSpec.typeDesc
    }

    private fun resolveParameterType(parameter: Parameter): TypeDesc {
        val declSpec = parameter.declspec.accept(this)
        return resolveParameterDeclaratorType(parameter.paramDeclarator, declSpec)
    }

    private fun resolveAbstractDeclaratorType(abstractDeclarator: AbstractDeclarator, typeDesc: TypeDesc): TypeDesc {
        val pointerType = wrapPointers(typeDesc.cType(), abstractDeclarator.pointers)
        var newTypeDesc = TypeDesc.from(pointerType)
        if (abstractDeclarator.directAbstractDeclarators == null) {
            return newTypeDesc
        }

        for (abstractDeclarator in abstractDeclarator.directAbstractDeclarators.reversed()) {
            newTypeDesc = when (abstractDeclarator) {
                is ArrayDeclarator -> resolveArrayDeclaratorType(abstractDeclarator, newTypeDesc)
                is ParameterTypeList -> resolveParameterTypeList(abstractDeclarator, newTypeDesc)
                is AbstractDeclarator -> resolveAbstractDeclaratorType(abstractDeclarator, newTypeDesc)
            }
        }

        return newTypeDesc
    }

    private fun resolveArrayDeclaratorType(arrayDeclarator: ArrayDeclarator, typeDesc: TypeDesc): TypeDesc {
        if (arrayDeclarator.constexpr is EmptyExpression) {
            return TypeDesc.from(CUncompletedArrayType(typeDesc))
        }

        val ctx = ArraySizeConstEvalContext(this)
        val size = ConstEvalExpression.eval(arrayDeclarator.constexpr, TryConstEvalExpressionLong(ctx))
            ?: return TypeDesc.from(CUncompletedArrayType(typeDesc))
        return TypeDesc.from(CArrayType(typeDesc, size))
    }

    private fun resolveParameterTypeList(parameterTypeList: ParameterTypeList, typeDesc: TypeDesc): TypeDesc {
        val params = resolveParamTypeListTypes(parameterTypeList)
        return TypeDesc.from(CFunctionType(typeDesc, params, parameterTypeList.isVarArg()), arrayListOf())
    }

    private fun resolveParamTypeListTypes(parameterTypeList: ParameterTypeList): List<TypeDesc> {
        if (parameterTypeList.params.size == 1) {
            val first = parameterTypeList.params[0]
            if (first !is Parameter) {
                return emptyList()
            }
            val type = resolveParameterType(first)
            // Special case for void
            // Pattern: 'void f(void)' can be found in the C program.
            return if (type.cType() == VOID) {
                emptyList()
            } else {
                listOf(type)
            }
        }

        val paramTypes = mutableListOf<TypeDesc>()
        for (param in parameterTypeList.params) {
            when (param) {
                is Parameter -> {
                    val type = resolveParameterType(param)
                    paramTypes.add(type)
                }
                is ParameterVarArg -> {}
            }
        }

        return paramTypes
    }

    /*
     * Expression visitors
     */

    private fun typeDescOfCompoundLiteral(compoundLiteral: CompoundLiteral): TypeDesc {
        val type = compoundLiteral.typeName.accept(this).typeDesc
        val ctype = type.cType()
        if (ctype is CUncompletedArrayType) {
            return TypeDesc.from(CArrayType(ctype.element(), compoundLiteral.initializerList.length().toLong()))
        }

        return type
    }

    override fun visit(expression: CompoundLiteral): CompletedType = memoize(expression) {
        val cType =typeDescOfCompoundLiteral(expression).cType()
        if (cType !is CompletedType) {
            throw TypeResolutionException("Compound literal on uncompleted type: $cType", expression.begin())
        }

        return@memoize cType
    }

    private fun convertToPrimitive(type: CompletedType): CPrimitive? = when (type) {
        is CPrimitive -> type
        is AnyCArrayType -> type.asPointer()
        is CFunctionType -> type.asPointer()
        else -> null
    }

    private fun convertToPointer(type: CompletedType): CPointer? = when (type) {
        is CPointer -> type
        is AnyCArrayType -> type.asPointer()
        else -> null
    }

    override fun visit(unaryOp: UnaryOp): CompletedType = memoize(unaryOp) {
        val primaryType = unaryOp.primary.accept(this)
        if (unaryOp.opType !is PrefixUnaryOpType) {
            return@memoize convertToPrimitive(primaryType)
                ?: throw TypeResolutionException("Unary operation '${unaryOp.opType}' on non-primitive type: $primaryType", unaryOp.begin())
        }

        return@memoize when (unaryOp.opType) {
            PrefixUnaryOpType.DEREF -> when (primaryType) {
                is CPointer      -> primaryType.dereference(unaryOp.begin(), typeHolder)
                is AnyCArrayType -> primaryType.completedType()
                    ?: throw TypeResolutionException("Array access on uncompleted type: $primaryType", unaryOp.begin())
                else -> throw TypeResolutionException("Dereference on non-pointer type: $primaryType", unaryOp.begin())
            }
            PrefixUnaryOpType.ADDRESS -> CPointer(primaryType)
            PrefixUnaryOpType.NOT -> INT
            PrefixUnaryOpType.NEG,
            PrefixUnaryOpType.INC,
            PrefixUnaryOpType.DEC,
            PrefixUnaryOpType.PLUS,
            PrefixUnaryOpType.BIT_NOT -> convertToPrimitive(primaryType)
                ?: throw TypeResolutionException("Unary operation '${unaryOp.opType}' on non-primitive type: $primaryType", unaryOp.begin())
        }
    }

    override fun visit(binop: BinaryOp): CompletedType = memoize(binop) {
        when (binop.opType) {
            OR, AND -> return@memoize BOOL
            COMMA -> return@memoize binop.right.accept(this)
            BinaryOpType.SHL,
            BinaryOpType.SHR -> return@memoize binop.left.accept(this)
            else -> {}
        }
        val l = binop.left.accept(this)
        val r = binop.right.accept(this)
        if (l == r) {
            return@memoize l
        }

        val leftType = convertToPrimitive(l)
            ?: throw TypeResolutionException("Binary operation on non-primitive type '$l': '${LineAgnosticAstPrinter.print(binop.left)}'", binop.begin())

        val rightType = convertToPrimitive(r)
            ?: throw TypeResolutionException("Binary operation on non-primitive type '$r': '${LineAgnosticAstPrinter.print(binop.right)}'", binop.begin())

        val resultType = leftType.interfere(rightType) ?:
            throw TypeResolutionException("Binary operation '$binop.opType' on incompatible types: $leftType and $rightType in ${binop.left.begin()}'", binop.begin())
        return@memoize resultType
    }

    override fun visit(conditional: Conditional): CompletedType = memoize(conditional) {
        val typeTrue  = conditional.eTrue.accept(this)
        val typeFalse = conditional.eFalse.accept(this)

        if (typeTrue is VOID || typeFalse is VOID) {
            return@memoize VOID
        }

        if (typeTrue is AnyCStructType && typeFalse is AnyCStructType && typeTrue == typeFalse) {
            return@memoize typeTrue
        }

        val cvtTypeTrue  = convertToPrimitive(typeTrue)
            ?: throw TypeResolutionException("Conditional with non-primitive types: $typeTrue and $typeFalse", conditional.begin())
        val cvtTypeFalse = convertToPrimitive(typeFalse)
            ?: throw TypeResolutionException("Conditional with non-primitive types: $typeTrue and $typeFalse", conditional.begin())

        if (cvtTypeTrue == cvtTypeFalse) {
            return@memoize cvtTypeTrue
        }

        return@memoize cvtTypeTrue.interfere(cvtTypeFalse) ?:
        throw TypeResolutionException("Conditional with incompatible types: $cvtTypeTrue and $cvtTypeFalse: '${LineAgnosticAstPrinter.print(conditional)}'", conditional.begin())
    }

    private fun resolveFunctionCallParams(functionCall: FunctionCall) {
        val params = functionCall.args.map { it.accept(this) }
        if (params.size != functionCall.args.size) {
            throw TypeResolutionException("Function call of '${LineAgnosticAstPrinter.print(functionCall.primary)}' with unresolved types", functionCall.begin())
        }

        for (i in functionCall.args.indices) {
            val argType = functionCall.args[i].accept(this)
            if (argType == params[i]) {
                continue
            }
            throw TypeResolutionException("Function call of '${LineAgnosticAstPrinter.print(functionCall.primary)}' with wrong argument types", functionCall.begin())
        }
    }

    private fun resolveFunctionCallType0(functionCall: FunctionCall): CPointer {
        val functionType = functionCall.primary.accept(this)
        if (functionType is CFunctionType) {
            return CPointer(functionType, setOf())
        }
        if (functionType !is CPointer) {
            throw TypeResolutionException("Function call with non-function type: $functionType", functionCall.begin())
        }
        return functionType
    }

    fun functionType(functionCall: FunctionCall): CFunctionType {
        resolveFunctionCallParams(functionCall)
        val functionType = if (functionCall.primary !is VarNode) {
            resolveFunctionCallType0(functionCall)
        } else {
            typeHolder.getFunctionType(functionCall.primary.name()).cType()
        }
        if (functionType is CPointer) {
            return functionType.dereference(functionCall.begin(), typeHolder).asType(functionCall.begin())
        }
        if (functionType !is CFunctionType) {
            throw TypeResolutionException("Function call of '' with non-function type", functionCall.begin())
        }

        return functionType
    }

    override fun visit(functionCall: FunctionCall): CompletedType = memoize(functionCall) {
        val cType = functionType(functionCall).retType().cType()
        if (cType !is CompletedType) {
            throw TypeResolutionException("Function call with uncompleted return type: $cType", functionCall.begin())
        }

        return cType
    }

    override fun visit(arrayAccess: ArrayAccess): CompletedType = memoize(arrayAccess) {
        return@memoize when (val primaryType = arrayAccess.primary.accept(this)) {
            is AnyCArrayType -> primaryType.completedType()
                ?: throw TypeResolutionException("Array access on uncompleted type: $primaryType", arrayAccess.begin())
            is CPointer -> primaryType.dereference(arrayAccess.begin(), typeHolder)
            is CPrimitive -> {
                val expressionType = arrayAccess.expr.accept(this)
                val exprPointer = convertToPointer(expressionType)
                    ?: throw TypeResolutionException("Array access with non-pointer type: $expressionType", arrayAccess.begin())
                exprPointer.dereference(arrayAccess.begin(), typeHolder)
            }
            else -> throw TypeResolutionException("Array access on non-array type: $primaryType", arrayAccess.begin())
        }
    }

    override fun visit(stringNode: StringNode): CompletedType = memoize(stringNode) {
        if (stringNode.data().isEmpty()) {
            return@memoize CStringLiteral( 1)
        }

        return@memoize CStringLiteral(stringNode.length().toLong())
    }

    override fun visit(assignment: CharNode): CompletedType = CHAR

    override fun visit(sizeOf: SizeOf): CompletedType = LONG

    private fun typeDefSpecifyCompleteType(typeName: TypeName): CompletedType {
        val cType = typeName.accept(this).typeDesc.cType()
        if (cType !is CompletedType) {
            throw TypeResolutionException("Uncompleted type: $cType", typeName.begin())
        }

        return cType
    }

    override fun visit(cast: Cast): CompletedType = memoize(cast) {
        return@memoize typeDefSpecifyCompleteType(cast.typeName)
    }

    override fun visit(numNode: NumNode): CompletedType = numNode.number.type

    override fun visit(varNode: VarNode): CompletedType = memoize(varNode) {
        val varType = typeHolder.getVarTypeOrNull(varNode.name())
        if (varType != null) {
            return@memoize varType.cType()
        }

        return@memoize typeHolder.findEnum(varNode.name()) ?: typeHolder.handleMissingVar(varNode)
    }

    override fun visit(arrowMemberAccess: ArrowMemberAccess): CompletedType = memoize(arrowMemberAccess) {
        val ty = arrowMemberAccess.primary.accept(this)
        val structType = convertToPointer(ty)
            ?: throw TypeResolutionException("Arrow member access on non-pointer type, but got $ty", arrowMemberAccess.begin())

        val baseType = structType.dereference(arrowMemberAccess.begin(), typeHolder)
        if (baseType !is AnyCStructType) {
            throw TypeResolutionException("Arrow member access on non-struct type, but got $baseType", arrowMemberAccess.begin())
        }

        val fieldDesc = baseType.fieldByNameOrNull(arrowMemberAccess.fieldName())
            ?: throw TypeResolutionException("Field ${arrowMemberAccess.fieldName()} not found in struct $baseType", arrowMemberAccess.begin())

        val cType = fieldDesc.cType()
        if (cType !is CompletedType) {
            throw TypeResolutionException("Arrow member access on uncompleted type: $cType", arrowMemberAccess.begin())
        }

        return@memoize cType
    }

    override fun visit(memberAccess: MemberAccess): CompletedType = memoize(memberAccess) {
        val structType = memberAccess.primary.accept(this)
        if (structType !is AnyCStructType) {
            throw TypeResolutionException("Member access on non-struct type, but got $structType", memberAccess.begin())
        }

        val fieldDesc = structType.fieldByNameOrNull(memberAccess.memberName()) //TODO deduplicate with arrowMemberAccess
            ?: throw TypeResolutionException("Field ${memberAccess.fieldName} not found in struct $structType", memberAccess.begin())

        val cType = fieldDesc.cType()
        if (cType !is CompletedType) {
            throw TypeResolutionException("Member access on uncompleted type: $cType", memberAccess.begin())
        }

        return@memoize cType
    }

    override fun visit(emptyExpression: EmptyExpression): CompletedType = VOID

    override fun visit(builtin: BuiltinVaArg): CompletedType = memoize(builtin) {
        return@memoize typeDefSpecifyCompleteType(builtin.typeName)
    }

    override fun visit(builtin: BuiltinVaStart): CompletedType = VOID

    override fun visit(builtin: BuiltinVaEnd): CompletedType = VOID

    override fun visit(builtin: BuiltinVaCopy): CompletedType = VOID

    // Resolve initializers

    private fun resolveInitializer(initializer: Initializer): CType = when (initializer) {
        is InitializerListInitializer -> resolveInitializerList(initializer.list)
        is ExpressionInitializer -> initializer.expr.accept(this)
    }

    fun resolveInitializerList(initializerList: InitializerList): CType {
        val types = arrayListOf<CType>()
        for (entry in initializerList.initializers) {
            types.add(resolveInitializer(entry.initializer()))
        }

        val baseTypes = arrayListOf<CType>()
        for (i in initializerList.initializers.indices) {
            baseTypes.add(types[i])
        }
        return if (baseTypes.size == 1 && baseTypes[0] is CStringLiteral) {
            baseTypes[0] //TODO is it needed?
        } else {
            InitializerType(baseTypes)
        }
    }

    override fun visit(specifierType: DeclarationSpecifier): DeclSpec = memoizeType(specifierType) {
        val typeBuilder = CTypeBuilder(specifierType.begin(), this)
        return@memoizeType typeBuilder.build(specifierType.specifiers)
    }

    override fun visit(typeName: TypeName): DeclSpec = memoizeType(typeName) {
        val specifierType = typeName.specifiers.accept(this)
        if (typeName.abstractDeclarator == null) {
            return specifierType
        }

        val typeDesc = resolveAbstractDeclaratorType(typeName.abstractDeclarator, specifierType.typeDesc)
        return DeclSpec(typeDesc, specifierType.storageClass)
    }

    companion object {
        fun default(): SemanticAnalysis {
            return SemanticAnalysis(TypeHolder.default())
        }
    }
}