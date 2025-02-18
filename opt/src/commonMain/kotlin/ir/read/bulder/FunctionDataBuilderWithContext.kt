package ir.read.bulder

import common.arrayFrom
import ir.value.*
import ir.types.*
import ir.module.*
import ir.instruction.*
import ir.read.tokens.*
import ir.module.block.*
import ir.module.builder.*
import common.forEachWith
import ir.attributes.ByValue
import ir.attributes.VarArgAttribute
import ir.value.constant.*


class ParseErrorException(message: String): Exception(message) {
    constructor(expect: String, token: Token):
            this( "${token.position()} found ${token.message()}, but expect $expect")
}

class FunctionDataBuilderWithContext private constructor(
    private val moduleBuilder: ModuleBuilderWithContext,
    prototype: FunctionPrototype,
    argumentValues: List<ArgumentValue>,
    private val nameMap: MutableMap<String, LocalValue>
): AnyFunctionDataBuilder(prototype, argumentValues) {
    private val nameToLabel = hashMapOf("entry" to bb)
    private val incompletePhis = arrayListOf<PhiContext>()

    private fun getValue(token: AnyValueToken, ty: Type): Value = when (token) {
        is LiteralValueToken -> when (token) {
            is IntValue       -> NonTrivialConstant.of(ty.asType(), token.int)
            is FloatValue     -> FloatingPointConstant.of(ty.asType(), token.fp)
            is BoolValueToken -> BoolValue.of(token.bool)
            is NULLValueToken -> NullValue
        }
        is LocalValueToken -> {
            val operand = nameMap[token.name]
                ?: throw ParseErrorException("in ${token.position()} undefined value '${token.value()}'")

            if (operand.type() != ty && operand.type() !is PtrType) {
                throw ParseErrorException("must be the same type: in_file=$ty, find=${operand.type()} in ${token.position()}")
            }

            operand
        }

        is SymbolValue -> {
            moduleBuilder.findConstantOrNull(token.name) ?:
                moduleBuilder.findGlobalOrNull(token.name) ?:
                    moduleBuilder.findFunctionOrNull(token.name) ?:
                    throw ParseErrorException("constant or global value", token)
        }
    }

    private fun getConstant(token: AnyValueToken, ty: NonTrivialType): Value = when (token) {
        is IntValue        -> NonTrivialConstant.of(ty, token.int)
        is FloatValue      -> NonTrivialConstant.of(ty, token.fp)
        is LocalValueToken -> UndefValue
        else -> throw ParseErrorException("constant or value", token)
    }

    private inline fun<reified T: Instruction> memorize(name: LocalValueToken, noinline value: InstBuilder<T>): T {
        val existed = nameMap[name.name]
        if (existed != null) {
            throw ParseErrorException("already has value with the same name=$existed in ${name.position()}")
        }

        val inst = bb.put(value)
        nameMap[name.name] = inst as LocalValue
        return inst
    }

    override fun build(): FunctionData {
        for (phi in incompletePhis) {
            phi.completePhi(nameMap)
        }

        normalizeBlocks()
        return fd
    }

    fun switchLabel(labelTok: LabelDefinition) {
        val label = getBlockOrCreate(labelTok.name)
        switchLabel(label)
    }

    fun neg(name: LocalValueToken, valueTok: AnyValueToken, expectedType: ArithmeticTypeToken): ArithmeticUnary {
        val value = getValue(valueTok, expectedType.type(moduleBuilder))
        return memorize(name, Neg.neg(value))
    }

    fun not(name: LocalValueToken, valueTok: AnyValueToken, expectedType: IntegerTypeToken): ArithmeticUnary {
        val value  = getValue(valueTok, expectedType.type())
        return memorize(name, Not.not(value))
    }

    private fun arithmeticBinary(name: LocalValueToken, a: AnyValueToken, b: AnyValueToken, expectedType: ArithmeticTypeToken, op: (Value, Value) -> InstBuilder<ArithmeticBinary>): ArithmeticBinary {
        val first  = getValue(a, expectedType.type(moduleBuilder))
        val second = getValue(b, expectedType.type(moduleBuilder))
        return memorize(name, op(first, second))
    }

    fun add(name: LocalValueToken, a: AnyValueToken, b: AnyValueToken, expectedType: ArithmeticTypeToken): ArithmeticBinary {
        return arithmeticBinary(name, a, b, expectedType, Add::add)
    }

    fun sub(name: LocalValueToken, a: AnyValueToken, b: AnyValueToken, expectedType: ArithmeticTypeToken): ArithmeticBinary {
        return arithmeticBinary(name, a, b, expectedType, Sub::sub)
    }

    fun mul(name: LocalValueToken, a: AnyValueToken, b: AnyValueToken, expectedType: ArithmeticTypeToken): ArithmeticBinary {
        return arithmeticBinary(name, a, b, expectedType, Mul::mul)
    }

    fun div(name: LocalValueToken, a: AnyValueToken, b: AnyValueToken, expectedType: FloatTypeToken): ArithmeticBinary {
        return arithmeticBinary(name, a, b, expectedType, Div::div)
    }

    fun shl(name: LocalValueToken, a: AnyValueToken, b: AnyValueToken, expectedType: IntegerTypeToken): ArithmeticBinary {
        return arithmeticBinary(name, a, b, expectedType, Shl::shl)
    }

    fun shr(name: LocalValueToken, a: AnyValueToken, b: AnyValueToken, expectedType: IntegerTypeToken): ArithmeticBinary {
        return arithmeticBinary(name, a, b, expectedType, Shr::shr)
    }

    fun and(name: LocalValueToken, a: AnyValueToken, b: AnyValueToken, expectedType: IntegerTypeToken): ArithmeticBinary {
        return arithmeticBinary(name, a, b, expectedType, And::and)
    }

    fun or(name: LocalValueToken, a: AnyValueToken, b: AnyValueToken, expectedType: ArithmeticTypeToken): ArithmeticBinary {
        return arithmeticBinary(name, a, b, expectedType, Or::or)
    }

    fun xor(name: LocalValueToken, a: AnyValueToken, b: AnyValueToken, expectedType: ArithmeticTypeToken): ArithmeticBinary {
        return arithmeticBinary(name, a, b, expectedType, Xor::xor)
    }

    fun tupleDiv(name: LocalValueToken, resultType: TupleTypeToken, a: AnyValueToken, b: AnyValueToken, expectedType: IntegerTypeToken): TupleDiv {
        val first  = getValue(a, expectedType.type())
        val second = getValue(b, expectedType.type())
        val result = TupleDiv.div(first, second)
        val inst = memorize(name, result)
        val resolvedResultType = resultType.type(moduleBuilder)
        if (resolvedResultType != inst.type()) {
            throw ParseErrorException("mismatch type: expected=$resolvedResultType, found=${inst.type()}")
        }

        return inst
    }

    private fun matchCompareType(predicate: Identifier): IntPredicate = when (predicate.string) {
        "eq" -> IntPredicate.Eq
        "ne" -> IntPredicate.Ne
        "ge" -> IntPredicate.Ge
        "gt" -> IntPredicate.Gt
        "lt" -> IntPredicate.Lt
        "le" -> IntPredicate.Le
        else  -> throw ParseErrorException("${predicate.position()} unknown compare type: cmpType=${predicate.string}")
    }

    fun icmp(name: LocalValueToken, a: AnyValueToken, predicate: Identifier, b: AnyValueToken, operandsTypes: PrimitiveTypeToken): IntCompare {
        val compareType = matchCompareType(predicate)

        val first  = getValue(a, operandsTypes.type())
        val second = getValue(b, operandsTypes.type())
        return memorize(name, IntCompare.icmp(first, compareType, second))
    }

    fun floatCompare(name: LocalValueToken, a: AnyValueToken, predicate: Identifier, b: AnyValueToken, operandsTypes: FloatTypeToken): FloatCompare {
        val compareType = when (predicate.string) {
            "oeq" -> FloatPredicate.Oeq
            "one" -> FloatPredicate.One
            "oge" -> FloatPredicate.Oge
            "ogt" -> FloatPredicate.Ogt
            "olt" -> FloatPredicate.Olt
            "ole" -> FloatPredicate.Ole
            "ugt" -> FloatPredicate.Ugt
            "uge" -> FloatPredicate.Uge
            "ult" -> FloatPredicate.Ult
            "ule" -> FloatPredicate.Ule
            "uno" -> FloatPredicate.Uno
            "ueq" -> FloatPredicate.Ueq
            "ord" -> FloatPredicate.Ord
            else  -> throw ParseErrorException("${predicate.position()} unknown compare type: cmpType=${predicate.string}")
        }

        val first  = getValue(a, operandsTypes.type())
        val second = getValue(b, operandsTypes.type())
        val result = FloatCompare.fcmp(first, compareType, second)
        return memorize(name, result)
    }

    fun load(name: LocalValueToken, ptr: AnyValueToken, expectedType: PrimitiveTypeToken): Load {
        val pointer = getValue(ptr, PtrType)
        return memorize(name, Load.load(expectedType.asType<PrimitiveType>(), pointer))
    }

    fun store(ptr: AnyValueToken, valueTok: AnyValueToken, expectedType: PrimitiveTypeToken) {
        val pointer = getValue(ptr, expectedType.type())
        val value   = getValue(valueTok, expectedType.asType<PrimitiveType>())
        bb.put(Store.store(pointer, value))
    }

    private fun convertToValues(types: List<Type>, args: List<AnyValueToken>): List<Value> {
        val argumentValues = arrayListOf<Value>()
        args.forEachWith(types) { arg, ty ->
            argumentValues.add(getValue(arg, ty))
        }
        return argumentValues
    }

    fun tupleCall(name: LocalValueToken, func: DirectFunctionPrototype, args: ArrayList<AnyValueToken>, target: LabelUsage): TupleCall {
        val argumentValues = convertToValues(func.arguments(), args)
        val block          = getBlockOrCreate(target.labelName)
        return memorize(name, TupleCall.call(func, argumentValues, hashSetOf(), block))
    }

    fun call(name: LocalValueToken, func: DirectFunctionPrototype, args: ArrayList<AnyValueToken>, labelUsage: LabelUsage): Value {
        require(func.returnType() !is VoidType)
        val argumentValues = convertToValues(func.arguments(), args)
        val block          = getBlockOrCreate(labelUsage.labelName)
        return memorize(name, Call.call(func, argumentValues, hashSetOf(), block))
    }

    fun vcall(func: DirectFunctionPrototype, args: ArrayList<AnyValueToken>, target: LabelUsage) {
        require(func.returnType() is VoidType)

        val argumentValues = convertToValues(func.arguments(), args)
        val block          = getBlockOrCreate(target.labelName)
        bb.put(VoidCall.call(func, argumentValues, hashSetOf(), block))
    }

    fun icall(name: LocalValueToken, pointerToken: ValueToken, func: IndirectFunctionPrototype, args: ArrayList<AnyValueToken>, labelUsage: LabelUsage): Value {
        require(func.returnType() !is VoidType)
        val argumentValues = convertToValues(func.arguments(), args)
        val pointer        = getValue(pointerToken, PtrType)
        val block          = getBlockOrCreate(labelUsage.labelName)

        return memorize(name, IndirectionCall.call(pointer, func, argumentValues, hashSetOf(), block))
    }

    fun ivcall(pointerToken: ValueToken, func: IndirectFunctionPrototype, args: ArrayList<AnyValueToken>, target: LabelUsage) {
        require(func.returnType() is VoidType)
        val argumentValues = convertToValues(func.arguments(), args)
        val pointer = getValue(pointerToken, PtrType)
        val output = getBlockOrCreate(target.labelName)
        bb.put(IndirectionVoidCall.call(pointer, func, argumentValues, hashSetOf(), output))
    }

    private fun getBlockOrCreate(name: String): Block {
        val target = nameToLabel[name]
        return if (target == null) {
            val new = allocateBlock()
            nameToLabel[name] = new
            new
        } else {
            target
        }
    }

    fun branch(targetName: LabelUsage) {
        val block = getBlockOrCreate(targetName.labelName)
        bb.put(Branch.br(block))
    }

    fun branchCond(valueTok: AnyValueToken, onTrueName: LabelUsage, onFalseName: LabelUsage) {
        val onTrue  = getBlockOrCreate(onTrueName.labelName)
        val onFalse = getBlockOrCreate(onFalseName.labelName)

        val value = getValue(valueTok, FlagType)
        bb.put(BranchCond.br(value, onTrue, onFalse))
    }

    fun alloc(name: LocalValueToken, ty: NonTrivialType): Alloc {
        return memorize(name, Alloc.alloc(ty))
    }

    fun ret(retValues: List<AnyValueToken>, expectedType: TypeToken) {
        val type  = expectedType.type(moduleBuilder)
        when (type) {
            is PrimitiveType -> bb.put(ReturnValue.ret(type, arrayFrom(retValues) { v -> getValue(v, type) }))
            is TupleType     -> bb.put(ReturnValue.ret(type, arrayFrom(retValues) { idx, v -> getValue(v, type.innerType(idx)) }))
            else -> throw ParseErrorException("primitive or tuple type", retValues[0])
        }
    }

    fun retVoid() = bb.put(ReturnVoid.ret())

    fun gep(name: LocalValueToken, type: PrimitiveTypeToken, sourceName: AnyValueToken, sourceType: PointerTypeToken, indexName: AnyValueToken, indexType: IntegerTypeToken): GetElementPtr {
        val source = getValue(sourceName, sourceType.type())
        val index  = getValue(indexName, indexType.type())
        return memorize(name, GetElementPtr.gep(source, type.asType<PrimitiveType>(), index))
    }

    fun gfp(name: LocalValueToken, type: AggregateTypeToken, sourceName: AnyValueToken, sourceType: PointerTypeToken, indexName: IntValue, indexType: IntegerTypeToken): GetFieldPtr {
        val source = getValue(sourceName, sourceType.type())
        val index  = getValue(indexName, indexType.type()) as IntegerConstant //TODO
        return memorize(name, GetFieldPtr.gfp(source, type.type(moduleBuilder), index))
    }

    fun bitcast(name: LocalValueToken, operandToken: AnyValueToken, operandType: PrimitiveTypeToken, resultType: PrimitiveTypeToken): Bitcast {
        val value = getValue(operandToken, operandType.type())
        return memorize(name, Bitcast.bitcast(value, resultType.asType()))
    }

    fun zext(name: LocalValueToken, operandToken: AnyValueToken, operandType: UnsignedIntegerTypeToken, resultType: UnsignedIntegerTypeToken): ZeroExtend {
        val value = getValue(operandToken, operandType.type())
        return memorize(name, ZeroExtend.zext(value,  resultType.type()))
    }

    fun sext(name: LocalValueToken, operandToken: AnyValueToken, operandType: SignedIntegerTypeToken, resultType: SignedIntegerTypeToken): SignExtend {
        val value = getValue(operandToken, operandType.type())
        return memorize(name, SignExtend.sext(value, resultType.type()))
    }

    fun trunc(name: LocalValueToken, operandToken: AnyValueToken, operandType: IntegerTypeToken, resultType: IntegerTypeToken): Truncate {
        val value = getValue(operandToken, operandType.type())
        return memorize(name, Truncate.trunc(value, resultType.type()))
    }

    fun fptrunc(name: LocalValueToken, operandToken: AnyValueToken, operandType: FloatTypeToken, resultType: FloatTypeToken): FpTruncate {
        val value = getValue(operandToken, operandType.type())
        return memorize(name, FpTruncate.fptrunc(value, resultType.type()))
    }

    fun fpext(name: LocalValueToken, operandToken: AnyValueToken, operandType: FloatTypeToken, resultType: FloatTypeToken): FpExtend {
        val value = getValue(operandToken, operandType.type())
        return memorize(name, FpExtend.fpext(value, resultType.type()))
    }

    fun fp2int(name: LocalValueToken, operandToken: AnyValueToken, operandType: FloatTypeToken, resultType: IntegerTypeToken): Float2Int {
        val value = getValue(operandToken, operandType.type())
        return memorize(name, Float2Int.fp2int(value, resultType.type()))
    }

    fun select(name: LocalValueToken, condTok: AnyValueToken, onTrueTok: AnyValueToken, onFalseTok: AnyValueToken, selectType: IntegerTypeToken): Value {
        val cond    = getValue(condTok, FlagType)
        val onTrue  = getValue(onTrueTok, selectType.type())
        val onFalse = getValue(onFalseTok, selectType.type())

        return memorize(name, Select.select(cond, selectType.type(), onTrue, onFalse))
    }

    fun flag2int(name: LocalValueToken, valueTok: AnyValueToken, expectedType: IntegerTypeToken): Flag2Int {
        val value = getValue(valueTok, FlagType)
        return memorize(name, Flag2Int.flag2int(value, expectedType.type()))
    }

    fun int2Float(name: LocalValueToken, valueTok: AnyValueToken, operandType: IntegerTypeToken, expectedType: FloatTypeToken): Int2Float {
        val value = getValue(valueTok, operandType.type())
        return memorize(name, Int2Float.int2fp(value, expectedType.type()))
    }

    fun uint2Float(name: LocalValueToken, valueTok: AnyValueToken, operandType: UnsignedIntegerTypeToken, expectedType: FloatTypeToken): Unsigned2Float {
        val value = getValue(valueTok, operandType.type())
        return memorize(name, Unsigned2Float.uint2fp(value, expectedType.type()))
    }

    fun ptr2int(name: LocalValueToken, valueTok: AnyValueToken, operandType: PointerTypeToken, intType: IntegerTypeToken): Pointer2Int {
        val value = getValue(valueTok, operandType.type())
        return memorize(name, Pointer2Int.ptr2int(value, intType.type()))
    }

    fun memcpy(dstTok: AnyValueToken, dstTypeTok: PointerTypeToken, srcTok: AnyValueToken, srcTyeToken: PointerTypeToken, lengthTok: UnsignedIntegerConstant) {
        val dst = getValue(dstTok, dstTypeTok.type())
        val src = getValue(srcTok, srcTyeToken.type())
        bb.put(Memcpy.memcpy(dst, src, lengthTok))
    }

    fun int2ptr(name: LocalValueToken, valueTok: AnyValueToken, intType: IntegerTypeToken): Int2Pointer {
        val value = getValue(valueTok, intType.type())
        return memorize(name, Int2Pointer.int2ptr(value))
    }

    fun switch(valueTok: AnyValueToken, defaultLabel: LabelUsage, integerType: IntegerTypeToken, table: List<IntValue>, targets: List<LocalValueToken>) {
        val value = getValue(valueTok, integerType.type())

        val targetBlocks  = arrayFrom(targets) { it -> getBlockOrCreate(it.name) }
        val tableConstant = arrayFrom(table) { it -> IntegerConstant.of(integerType.type(), it.int) }

        val defaultLabelResolved = getBlockOrCreate(defaultLabel.labelName)
        bb.put(Switch.switch(value, defaultLabelResolved, tableConstant, targetBlocks))
    }

    fun phi(name: LocalValueToken, incomingTok: ArrayList<AnyValueToken>, labelsTok: ArrayList<Identifier>, expectedType: PrimitiveTypeToken): Value {
        val blocks = arrayFrom(labelsTok) { it -> getBlockOrCreate(it.string) }
        val type = expectedType.asType<PrimitiveType>()
        val values = arrayFrom(incomingTok) { tok -> getConstant(tok, type) }

        val phi = Phi.phi(blocks, type, values)
        val inst = memorize(name, phi)
        incompletePhis.add(PhiContext(inst, incomingTok, type))
        return inst
    }

    fun proj(name: LocalValueToken, typeToken: TupleTypeToken, valueTok: AnyValueToken, expectedType: PrimitiveTypeToken, index: IntValue): Projection {
        val value = getValue(valueTok, typeToken.type(moduleBuilder))
        //if (value !is TupleInstruction) {
        //    throw ParseErrorException("tuple type", valueTok)
        //}

        return memorize(name, Projection.proj(value, index.int.toInt()))
    }

    fun makePrototype(functionName: SymbolValue, returnType: Type, argTypes: List<TypeToken>): FunctionPrototype {
        val types = moduleBuilder.resolveArgumentType(argTypes)
        val attributes = if (argTypes.lastOrNull() is Vararg) {
            hashSetOf(VarArgAttribute)
        } else {
            emptySet()
        }
        return FunctionPrototype(functionName.name, returnType, types, attributes)
    }

    fun makePrototype(returnType: Type, argTypes: List<TypeToken>): IndirectFunctionPrototype {
        val types = moduleBuilder.resolveArgumentType(argTypes)
        val attributes = if (argTypes.lastOrNull() is Vararg) {
            hashSetOf(VarArgAttribute)
        } else {
            emptySet()
        }
        return IndirectFunctionPrototype(returnType, types, attributes)
    }

    companion object {
        fun create(moduleBuilder: ModuleBuilderWithContext, prototype: FunctionPrototype,
                   argumentValueTokens: List<LocalValueToken>): FunctionDataBuilderWithContext {
            fun handleArguments(argumentTypeTokens: List<Type>): List<ArgumentValue> {
                val argumentValues = arrayListOf<ArgumentValue>()
                for ((idx, arg) in argumentTypeTokens.withIndex()) {
                    if (arg !is PrimitiveType) { //TODO vararg
                        continue
                    }

                    val byValue = prototype.attributes.find { it is ByValue && it.argumentIndex == idx } // TODO: simplify!!???!
                    val argAttr = if (byValue != null) {
                        hashSetOf(byValue as ByValue)
                    } else {
                        hashSetOf()
                    }
                    argumentValues.add(ArgumentValue(idx, arg, argAttr))
                }

                return argumentValues
            }

            fun setupNameMap(argument: List<ArgumentValue>, tokens: List<LocalValueToken>): MutableMap<String, LocalValue> {
                val nameToValue = hashMapOf<String, LocalValue>()
                argument.forEachWith(tokens) { arg, tok ->
                    nameToValue[tok.name] = arg
                }

                return nameToValue
            }

            val arguments = handleArguments(prototype.arguments())
            val nameMap   = setupNameMap(arguments, argumentValueTokens)

            return FunctionDataBuilderWithContext(moduleBuilder, prototype, arguments, nameMap)
        }
    }
}

private data class PhiContext(val phi: Phi, val valueTokens: List<AnyValueToken>, val expectedType: PrimitiveType) {
    fun completePhi(valueMap: Map<String, LocalValue>) {
        for ((idx, tok) in valueTokens.withIndex()) {
            if (tok !is LocalValueToken) {
                continue
            }

            val local = valueMap[tok.name]
                ?: throw ParseErrorException("undefined value ${tok.name} in ${tok.position()}")

            if (local.type() != expectedType) {
                throw ParseErrorException("mismatch type ${local.type()} in ${tok.position()}")
            }
            phi.owner().updateDF(phi, idx, local)
        }
    }
}