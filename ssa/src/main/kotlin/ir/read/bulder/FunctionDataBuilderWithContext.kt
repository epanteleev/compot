package ir.read.bulder

import ir.*
import ir.read.*
import ir.types.*
import ir.module.*
import ir.instruction.*
import ir.module.block.*
import ir.module.builder.*
import common.forEachWith
import ir.BoolValue


class ParseErrorException(message: String): Exception(message) {
    constructor(expect: String, token: Token):
            this( "${token.position()} found: ${token.message()}, but expect $expect")
}

class FunctionDataBuilderWithContext private constructor(
    private val moduleBuilder: ModuleBuilderWithContext,
    prototype: FunctionPrototype,
    argumentValues: List<ArgumentValue>,
    blocks: BasicBlocks,
    private val nameMap: MutableMap<String, LocalValue>
): AnyFunctionDataBuilder(prototype, argumentValues, blocks) {
    private val nameToLabel = hashMapOf("entry" to bb)
    private val incompletePhis = arrayListOf<PhiContext>()

    private fun getValue(token: AnyValueToken, ty: Type): Value {
        return when (token) {
            is IntValue   -> Constant.of(ty, token.int)
            is FloatValue -> Constant.of(ty, token.fp)
            is BoolValueToken  -> BoolValue.of(token.bool)
            is NULLValueToken  -> NullValue.NULLPTR
            is LocalValueToken -> {
                val operand = nameMap[token.name]
                    ?: throw ParseErrorException("in ${token.position()} undefined value '${token.value()}'")

                if (operand.type() != ty && operand.type() !is PointerType) {
                    throw ParseErrorException("must be the same type: in_file=$ty, find=${operand.type()} in ${token.position()}")
                }

                operand
            }

            is SymbolValue -> moduleBuilder.findGlobal(token.name)
            else -> throw ParseErrorException("constant or value", token)
        }
    }

    private fun getConstant(token: AnyValueToken, ty: Type): Value {
        return token.let {
            when (it) {
                is IntValue        -> Constant.of(ty, it.int)
                is FloatValue      -> Constant.of(ty, it.fp)
                is LocalValueToken -> Value.UNDEF
                else -> throw ParseErrorException("constant or value", it)
            }
        }
    }

    private inline fun<reified T: ValueInstruction> memorize(name: LocalValueToken, value: T): T {
        val existed = nameMap[name.name]
        if (existed != null) {
            throw ParseErrorException("already has value with the same name=$existed in ${name.position()}")
        }

        nameMap[name.name] = value
        return value
    }

    override fun build(): FunctionData {
        for (phi in incompletePhis) {
            phi.completePhi(nameMap)
        }

        normalizeBlocks()
        return FunctionData.create(prototype, blocks, argumentValues)
    }

    fun switchLabel(labelTok: LabelDefinition) {
        val label = getBlockOrCreate(labelTok.name)
        bb = blocks.findBlock(label)
    }

    fun neg(name: LocalValueToken, valueTok: AnyValueToken, expectedType: ArithmeticTypeToken): ArithmeticUnary {
        val value = getValue(valueTok, expectedType.type(moduleBuilder))
        return memorize(name, bb.neg(value))
    }

    fun not(name: LocalValueToken, valueTok: AnyValueToken, expectedType: IntegerTypeToken): ArithmeticUnary {
        val value  = getValue(valueTok, expectedType.type())
        return memorize(name, bb.not(value))
    }

    fun arithmeticBinary(name: LocalValueToken, a: AnyValueToken, op: ArithmeticBinaryOp, b: AnyValueToken, expectedType: ArithmeticTypeToken): ArithmeticBinary {
        val first  = getValue(a, expectedType.type(moduleBuilder))
        val second = getValue(b, expectedType.type(moduleBuilder))
        val result = bb.arithmeticBinary(first, op, second)
        return memorize(name, result)
    }

    private fun matchCompareType(predicate: Identifier): IntPredicate {
        return when (predicate.string) {
            "eq" -> IntPredicate.Eq
            "ne" -> IntPredicate.Ne
            "ge" -> IntPredicate.Ge
            "gt" -> IntPredicate.Gt
            "lt" -> IntPredicate.Lt
            "le" -> IntPredicate.Le
            else  -> throw ParseErrorException("${predicate.position()} unknown compare type: cmpType=${predicate.string}")
        }
    }

    fun icmp(name: LocalValueToken, a: AnyValueToken, predicate: Identifier, b: AnyValueToken, operandsTypes: SignedIntegerTypeToken): SignedIntCompare {
        val compareType = matchCompareType(predicate)

        val first  = getValue(a, operandsTypes.type())
        val second = getValue(b, operandsTypes.type())
        return memorize(name, bb.icmp(first, compareType, second))
    }

    fun ucmp(name: LocalValueToken, a: AnyValueToken, predicate: Identifier, b: AnyValueToken, operandsTypes: UnsignedIntegerTypeToken): UnsignedIntCompare {
        val compareType = matchCompareType(predicate)

        val first  = getValue(a, operandsTypes.type())
        val second = getValue(b, operandsTypes.type())
        return memorize(name, bb.ucmp(first, compareType, second))
    }

    fun pcmp(name: LocalValueToken, a: AnyValueToken, predicate: Identifier, b: AnyValueToken, operandsTypes: PointerTypeToken): PointerCompare {
        val compareType = matchCompareType(predicate)

        val first  = getValue(a, operandsTypes.type())
        val second = getValue(b, operandsTypes.type())
        return memorize(name, bb.pcmp(first, compareType, second))
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
        val result = bb.fcmp(first, compareType, second)
        return memorize(name, result)
    }

    fun load(name: LocalValueToken, ptr: AnyValueToken, expectedType: PrimitiveTypeToken): Load {
        val pointer = getValue(ptr, Type.Ptr)
        return memorize(name, bb.load(expectedType.asType<PrimitiveType>(),pointer))
    }

    fun store(ptr: AnyValueToken, valueTok: AnyValueToken, expectedType: PrimitiveTypeToken) {
        val pointer = getValue(ptr, expectedType.type())
        val value   = getValue(valueTok, expectedType.asType<PrimitiveType>())
        bb.store(pointer, value)
    }

    private fun convertToValues(types: List<Type>, args: List<AnyValueToken>): List<Value> {
        val argumentValues = arrayListOf<Value>()
        args.forEachWith(types) { arg, ty ->
            argumentValues.add(getValue(arg, ty))
        }
        return argumentValues
    }

    fun call(name: LocalValueToken, func: AnyFunctionPrototype, args: ArrayList<AnyValueToken>): Value {
        require(func.returnType() !is VoidType)
        val argumentValues = convertToValues(func.arguments(), args)
        return memorize(name, bb.call(func, argumentValues))
    }

    fun vcall(func: AnyFunctionPrototype, args: ArrayList<AnyValueToken>) {
        require(func.returnType() is VoidType)
        val argumentValues = convertToValues(func.arguments(), args)
        bb.vcall(func, argumentValues)
    }

    fun icall(name: LocalValueToken, pointerToken: ValueToken, func: IndirectFunctionPrototype, args: ArrayList<AnyValueToken>): Value {
        require(func.returnType() !is VoidType)
        val argumentValues = convertToValues(func.arguments(), args)
        val pointer = getValue(pointerToken, Type.Ptr)
        return memorize(name, bb.icall(pointer, func, argumentValues))
    }

    fun ivcall(pointerToken: ValueToken, func: IndirectFunctionPrototype, args: ArrayList<AnyValueToken>) {
        require(func.returnType() is VoidType)
        val argumentValues = convertToValues(func.arguments(), args)
        val pointer = getValue(pointerToken, Type.Ptr)
        bb.ivcall(pointer, func,argumentValues)
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
        bb.branch(block)
    }

    fun branchCond(valueTok: AnyValueToken, onTrueName: LabelUsage, onFalseName: LabelUsage) {
        val onTrue  = getBlockOrCreate(onTrueName.labelName)
        val onFalse = getBlockOrCreate(onFalseName.labelName)

        val value = getValue(valueTok, Type.U1)
        bb.branchCond(value, onTrue, onFalse)
    }

    fun alloc(name: LocalValueToken, ty: NonTrivialType): Alloc {
        return memorize(name, bb.alloc(ty))
    }

    fun ret(retValue: AnyValueToken, expectedType: TypeToken) {
        val value = getValue(retValue, expectedType.type(moduleBuilder))
        bb.ret(value)
    }

    fun retVoid() = bb.retVoid()

    fun gep(name: LocalValueToken, type: PrimitiveTypeToken, sourceName: AnyValueToken, sourceType: PointerTypeToken, indexName: AnyValueToken, indexType: IntegerTypeToken): GetElementPtr {
        val source = getValue(sourceName, sourceType.type())
        val index  = getValue(indexName, indexType.type())
        return memorize(name, bb.gep(source, type.asType<PrimitiveType>(), index))
    }

    fun gfp(name: LocalValueToken, type: AggregateTypeToken, sourceName: AnyValueToken, sourceType: PointerTypeToken, indexName: IntValue, indexType: IntegerTypeToken): GetFieldPtr {
        val source = getValue(sourceName, sourceType.type())
        val index  = getValue(indexName, indexType.type()) as IntegerConstant
        return memorize(name, bb.gfp(source, type.type(moduleBuilder), index))
    }

    fun bitcast(name: LocalValueToken, operandToken: AnyValueToken, operandType: PrimitiveTypeToken, resultType: PrimitiveTypeToken): Bitcast {
        val value = getValue(operandToken, operandType.type())
        return memorize(name, bb.bitcast(value, resultType.type()))
    }

    fun zext(name: LocalValueToken, operandToken: AnyValueToken, operandType: UnsignedIntegerTypeToken, resultType: UnsignedIntegerTypeToken): ZeroExtend {
        val value = getValue(operandToken, operandType.type())
        return memorize(name, bb.zext(value,  resultType.type()))
    }

    fun sext(name: LocalValueToken, operandToken: AnyValueToken, operandType: SignedIntegerTypeToken, resultType: SignedIntegerTypeToken): SignExtend {
        val value = getValue(operandToken, operandType.type())
        return memorize(name, bb.sext(value, resultType.type()))
    }

    fun trunc(name: LocalValueToken, operandToken: AnyValueToken, operandType: IntegerTypeToken, resultType: IntegerTypeToken): Truncate {
        val value = getValue(operandToken, operandType.type())
        return memorize(name, bb.trunc(value, resultType.type()))
    }

    fun fptrunc(name: LocalValueToken, operandToken: AnyValueToken, operandType: FloatTypeToken, resultType: FloatTypeToken): FpTruncate {
        val value = getValue(operandToken, operandType.type())
        return memorize(name, bb.fptrunc(value, resultType.type()))
    }

    fun fpext(name: LocalValueToken, operandToken: AnyValueToken, operandType: FloatTypeToken, resultType: FloatTypeToken): FpExtend {
        val value = getValue(operandToken, operandType.type())
        return memorize(name, bb.fpext(value, resultType.type()))
    }

    fun fp2int(name: LocalValueToken, operandToken: AnyValueToken, operandType: FloatTypeToken, resultType: IntegerTypeToken): FloatToInt {
        val value = getValue(operandToken, operandType.type())
        return memorize(name, bb.fp2Int(value, resultType.type()))
    }

    fun select(name: LocalValueToken, condTok: AnyValueToken, onTrueTok: AnyValueToken, onFalseTok: AnyValueToken, selectType: PrimitiveTypeToken): Value {
        val cond    = getValue(condTok, Type.U1)
        val onTrue  = getValue(onTrueTok, selectType.type())
        val onFalse = getValue(onFalseTok, selectType.type())

        return memorize(name, bb.select(cond, selectType.type(), onTrue, onFalse))
    }

    fun flag2int(name: LocalValueToken, valueTok: AnyValueToken, expectedType: IntegerTypeToken): Flag2Int {
        val value = getValue(valueTok, Type.U1)
        return memorize(name, bb.flag2int(value, expectedType.type()))
    }

    fun int2Float(name: LocalValueToken, valueTok: AnyValueToken, operandType: IntegerTypeToken, expectedType: FloatTypeToken): Int2Float {
        val value = getValue(valueTok, operandType.type())
        return memorize(name, bb.int2fp(value, expectedType.type()))
    }

    fun ptr2int(name: LocalValueToken, valueTok: AnyValueToken, operandType: PointerTypeToken, intType: IntegerTypeToken): Pointer2Int {
        val value = getValue(valueTok, operandType.type())
        return memorize(name, bb.ptr2int(value, intType.type()))
    }

    fun memcpy(dstTok: AnyValueToken, dstTypeTok: PointerTypeToken, srcTok: AnyValueToken, srcTyeToken: PointerTypeToken, lengthTok: UnsignedIntegerConstant) {
        val dst = getValue(dstTok, dstTypeTok.type())
        val src = getValue(srcTok, srcTyeToken.type())
        bb.memcpy(dst, src, lengthTok)
    }

    fun int2ptr(name: LocalValueToken, valueTok: AnyValueToken, intType: IntegerTypeToken): Int2Pointer {
        val value = getValue(valueTok, intType.type())
        return memorize(name, bb.int2ptr(value))
    }

    fun phi(name: LocalValueToken, incomingTok: ArrayList<AnyValueToken>, labelsTok: ArrayList<Identifier>, expectedType: PrimitiveTypeToken): Value {
        val blocks = arrayListOf<Block>()
        for (tok in labelsTok) {
            blocks.add(getBlockOrCreate(tok.string))
        }

        val type = expectedType.asType<PrimitiveType>()
        val values = arrayListOf<Value>()
        for (tok in incomingTok) {
            values.add(getConstant(tok, type))
        }

        val phi = bb.uncompletedPhi(type, values, blocks)
        incompletePhis.add(PhiContext(phi, incomingTok, type))
        return memorize(name, phi)
    }

    fun makePrototype(functionName: SymbolValue, returnType: TypeToken, argTypes: List<TypeToken>): FunctionPrototype {
        val types = argTypes.mapTo(arrayListOf()) { it.type(moduleBuilder) }
        return FunctionPrototype(functionName.name, returnType.type(moduleBuilder), types)
    }

    fun makePrototype(returnType: TypeToken, argTypes: List<TypeToken>): IndirectFunctionPrototype {
        val types = argTypes.mapTo(arrayListOf()) { it.type(moduleBuilder) }
        return IndirectFunctionPrototype(returnType.type(moduleBuilder), types)
    }

    companion object {
        fun create(moduleBuilder: ModuleBuilderWithContext, prototype: FunctionPrototype,
                   argumentValueTokens: List<LocalValueToken>): FunctionDataBuilderWithContext {
            fun handleArguments(argumentTypeTokens: List<Type>): List<ArgumentValue> {
                val argumentValues = arrayListOf<ArgumentValue>()
                for ((idx, arg) in argumentTypeTokens.withIndex()) {
                    if (arg !is NonTrivialType) {
                        continue
                    }

                    argumentValues.add(ArgumentValue(idx, arg))
                }

                return argumentValues
            }

            fun setupNameMap(argument: List<ArgumentValue>, tokens: List<LocalValueToken>): MutableMap<String, LocalValue> {
                val nameToValue = hashMapOf<String, LocalValue>()
                for ((arg, tok) in argument zip tokens) {
                    nameToValue[tok.name] = arg
                }

                return nameToValue
            }

            val startBB     = Block.empty(Label.entry.index)
            val basicBlocks = BasicBlocks.create(startBB)

            val arguments = handleArguments(prototype.arguments())
            val nameMap   = setupNameMap(arguments, argumentValueTokens)

            return FunctionDataBuilderWithContext(moduleBuilder, prototype, arguments, basicBlocks, nameMap)
        }
    }
}

private data class PhiContext(val phi: Phi, val valueTokens: List<AnyValueToken>, val expectedType: PrimitiveType) {
    fun completePhi(valueMap: Map<String, LocalValue>) {
        val values = phi.operands()
        for ((idx, tok) in valueTokens.withIndex()) {
            if (tok !is LocalValueToken) {
                continue
            }

            val local = valueMap[tok.name]
                ?: throw ParseErrorException("undefined value ${tok.name} in ${tok.position()}")

            if (local.type() != expectedType) {
                throw ParseErrorException("mismatch type ${local.type()} in ${tok.position()}")
            }
            values[idx] = local
        }
    }
}