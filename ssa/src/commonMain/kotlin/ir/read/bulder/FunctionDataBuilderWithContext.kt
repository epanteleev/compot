package ir.read.bulder

import ir.*
import ir.types.*
import ir.module.*
import ir.instruction.*
import ir.module.block.*
import ir.module.builder.*
import common.forEachWith
import ir.read.tokens.*
import ir.value.*


class ParseErrorException(message: String): Exception(message) {
    constructor(expect: String, token: Token):
            this( "${token.position()} found: ${token.message()}, but expect $expect")
}

class FunctionDataBuilderWithContext private constructor(
    private val moduleBuilder: ModuleBuilderWithContext,
    prototype: FunctionPrototype,
    argumentValues: List<_root_ide_package_.ir.value.ArgumentValue>,
    blocks: BasicBlocks,
    private val nameMap: MutableMap<String, _root_ide_package_.ir.value.LocalValue>
): AnyFunctionDataBuilder(prototype, argumentValues, blocks) {
    private val nameToLabel = hashMapOf("entry" to bb)
    private val incompletePhis = arrayListOf<PhiContext>()

    private fun getValue(token: AnyValueToken, ty: Type): _root_ide_package_.ir.value.Value {
        return when (token) {
            is LiteralValueToken -> token.toConstant(ty)
            is LocalValueToken -> {
                val operand = nameMap[token.name]
                    ?: throw ParseErrorException("in ${token.position()} undefined value '${token.value()}'")

                if (operand.type() != ty && operand.type() !is PointerType) {
                    throw ParseErrorException("must be the same type: in_file=$ty, find=${operand.type()} in ${token.position()}")
                }

                operand
            }

            is SymbolValue -> {
                moduleBuilder.findConstantOrNull(token.name) ?:
                    moduleBuilder.findGlobalOrNull(token.name) ?: moduleBuilder.findFunctionOrNull(token.name) ?:
                        throw ParseErrorException("constant or global value", token)
            }
            else -> throw ParseErrorException("constant or value", token)
        }
    }

    private fun getConstant(token: AnyValueToken, ty: Type): _root_ide_package_.ir.value.Value {
        return token.let {
            when (it) {
                is IntValue        -> _root_ide_package_.ir.value.Constant.of(ty, it.int)
                is FloatValue      -> _root_ide_package_.ir.value.Constant.of(ty, it.fp)
                is LocalValueToken -> _root_ide_package_.ir.value.Value.UNDEF
                else -> throw ParseErrorException("constant or value", it)
            }
        }
    }

    private inline fun<reified T: _root_ide_package_.ir.value.LocalValue> memorize(name: LocalValueToken, value: T): T {
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

    fun tupleDiv(name: LocalValueToken, resultType: TupleTypeToken, a: AnyValueToken, b: AnyValueToken, expectedType: IntegerTypeToken): TupleDiv {
        val first  = getValue(a, expectedType.type())
        val second = getValue(b, expectedType.type())
        val result = bb.tupleDiv(first, second)
        val resolvedResultType = resultType.type(moduleBuilder)
        if (resolvedResultType != result.type()) {
            throw ParseErrorException("mismatch type: expected=$resolvedResultType, found=${result.type()}")
        }

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

    private fun convertToValues(types: List<Type>, args: List<AnyValueToken>): List<_root_ide_package_.ir.value.Value> {
        val argumentValues = arrayListOf<_root_ide_package_.ir.value.Value>()
        args.forEachWith(types) { arg, ty ->
            argumentValues.add(getValue(arg, ty))
        }
        return argumentValues
    }

    fun call(name: LocalValueToken, func: AnyFunctionPrototype, args: ArrayList<AnyValueToken>, labelUsage: LabelUsage): _root_ide_package_.ir.value.Value {
        require(func.returnType() !is VoidType)
        val argumentValues = convertToValues(func.arguments(), args)
        val block          = getBlockOrCreate(labelUsage.labelName)
        return memorize(name, bb.call(func, argumentValues, block))
    }

    fun vcall(func: AnyFunctionPrototype, args: ArrayList<AnyValueToken>, target: LabelUsage) {
        require(func.returnType() is VoidType)

        val argumentValues = convertToValues(func.arguments(), args)
        val block          = getBlockOrCreate(target.labelName)
        bb.vcall(func, argumentValues, block)
    }

    fun icall(name: LocalValueToken, pointerToken: ValueToken, func: IndirectFunctionPrototype, args: ArrayList<AnyValueToken>, labelUsage: LabelUsage): _root_ide_package_.ir.value.Value {
        require(func.returnType() !is VoidType)
        val argumentValues = convertToValues(func.arguments(), args)
        val pointer        = getValue(pointerToken, Type.Ptr)
        val block          = getBlockOrCreate(labelUsage.labelName)

        return memorize(name, bb.icall(pointer, func, argumentValues, block))
    }

    fun ivcall(pointerToken: ValueToken, func: IndirectFunctionPrototype, args: ArrayList<AnyValueToken>, target: LabelUsage) {
        require(func.returnType() is VoidType)
        val argumentValues = convertToValues(func.arguments(), args)
        val pointer = getValue(pointerToken, Type.Ptr)
        val output = getBlockOrCreate(target.labelName)
        bb.ivcall(pointer, func, argumentValues, output)
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
        val index  = getValue(indexName, indexType.type()) as _root_ide_package_.ir.value.IntegerConstant
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

    fun select(name: LocalValueToken, condTok: AnyValueToken, onTrueTok: AnyValueToken, onFalseTok: AnyValueToken, selectType: PrimitiveTypeToken): _root_ide_package_.ir.value.Value {
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

    fun memcpy(dstTok: AnyValueToken, dstTypeTok: PointerTypeToken, srcTok: AnyValueToken, srcTyeToken: PointerTypeToken, lengthTok: _root_ide_package_.ir.value.UnsignedIntegerConstant) {
        val dst = getValue(dstTok, dstTypeTok.type())
        val src = getValue(srcTok, srcTyeToken.type())
        bb.memcpy(dst, src, lengthTok)
    }

    fun int2ptr(name: LocalValueToken, valueTok: AnyValueToken, intType: IntegerTypeToken): Int2Pointer {
        val value = getValue(valueTok, intType.type())
        return memorize(name, bb.int2ptr(value))
    }

    fun phi(name: LocalValueToken, incomingTok: ArrayList<AnyValueToken>, labelsTok: ArrayList<Identifier>, expectedType: PrimitiveTypeToken): _root_ide_package_.ir.value.Value {
        val blocks = arrayListOf<Block>()
        for (tok in labelsTok) {
            blocks.add(getBlockOrCreate(tok.string))
        }

        val type = expectedType.asType<PrimitiveType>()
        val values = arrayListOf<_root_ide_package_.ir.value.Value>()
        for (tok in incomingTok) {
            values.add(getConstant(tok, type))
        }

        val phi = bb.uncompletedPhi(type, values, blocks)
        incompletePhis.add(PhiContext(phi, incomingTok, type))
        return memorize(name, phi)
    }

    fun proj(name: LocalValueToken, typeToken: TupleTypeToken, valueTok: AnyValueToken, expectedType: PrimitiveTypeToken, index: IntValue): Projection {
        val value = getValue(valueTok, typeToken.type(moduleBuilder))
        if (value !is TupleInstruction) {
            throw ParseErrorException("tuple type", valueTok)
        }

        return memorize(name, bb.proj(value, index.int.toInt()))
    }

    fun makePrototype(functionName: SymbolValue, returnType: TypeToken, argTypes: List<TypeToken>): FunctionPrototype {
        val types = moduleBuilder.resolveArgumentType(argTypes)
        val isVararg = argTypes.lastOrNull() is Vararg
        return FunctionPrototype(functionName.name, returnType.type(moduleBuilder), types, isVararg)
    }

    fun makePrototype(returnType: TypeToken, argTypes: List<TypeToken>): IndirectFunctionPrototype {
        val types = moduleBuilder.resolveArgumentType(argTypes)
        val isVararg = argTypes.lastOrNull() is Vararg
        return IndirectFunctionPrototype(returnType.type(moduleBuilder), types, isVararg)
    }

    companion object {
        fun create(moduleBuilder: ModuleBuilderWithContext, prototype: FunctionPrototype,
                   argumentValueTokens: List<LocalValueToken>): FunctionDataBuilderWithContext {
            fun handleArguments(argumentTypeTokens: List<Type>): List<_root_ide_package_.ir.value.ArgumentValue> {
                val argumentValues = arrayListOf<_root_ide_package_.ir.value.ArgumentValue>()
                for ((idx, arg) in argumentTypeTokens.withIndex()) {
                    if (arg !is NonTrivialType) {
                        continue
                    }

                    argumentValues.add(_root_ide_package_.ir.value.ArgumentValue(idx, arg))
                }

                return argumentValues
            }

            fun setupNameMap(argument: List<_root_ide_package_.ir.value.ArgumentValue>, tokens: List<LocalValueToken>): MutableMap<String, _root_ide_package_.ir.value.LocalValue> {
                val nameToValue = hashMapOf<String, _root_ide_package_.ir.value.LocalValue>()
                argument.forEachWith(tokens) { arg, tok ->
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
    fun completePhi(valueMap: Map<String, _root_ide_package_.ir.value.LocalValue>) {
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