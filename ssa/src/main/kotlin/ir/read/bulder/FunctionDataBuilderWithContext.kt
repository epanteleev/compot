package ir.read.bulder

import ir.*
import ir.instruction.*
import ir.module.BasicBlocks
import ir.module.FunctionData
import ir.module.block.Block
import ir.module.block.Label
import ir.read.*
import ir.types.*

class ParseErrorException(message: String): Exception(message) {
    constructor(expect: String, token: Token):
            this( "${token.position()} found: ${token.message()}, but expect $expect")
}

class FunctionDataBuilderWithContext private constructor(
    private val prototype: FunctionPrototype,
    private val argumentValues: List<ArgumentValue>,
    private val blocks: BasicBlocks,
    private val nameMap: MutableMap<String, LocalValue>,
    private val globals: Set<GlobalValue>
) {
    private var allocatedLabel: Int = 0
    private var bb: Block = blocks.begin()
    private val nameToLabel = hashMapOf("entry" to bb)
    private val incomplitedPhi = arrayListOf<PhiContext>()

    private fun allocateBlock(): Block {
        allocatedLabel += 1
        val bb = Block.empty(allocatedLabel)
        blocks.putBlock(bb)
        return bb
    }

    private fun getValue(token: AnyValueToken, ty: Type): Value {
        return when (token) {
            is IntValue   -> Constant.of(ty, token.int)
            is FloatValue -> Constant.of(ty, token.fp)
            is LocalValueToken -> {
                val operand = nameMap[token.name]
                    ?: throw RuntimeException("in ${token.position()} undefined value ${token.name}")

                if (operand.type() != ty && operand.type() !is PointerType) {
                    throw ParseErrorException("must be the same type: in_file=$ty, find=${operand.type()} in ${token.position()}")
                }

                operand
            }

            is SymbolValue -> {
                val symbolName = token.name
                val gValue = globals.find { symbolName == it.name() }
                    ?: throw ParseErrorException("cannot find global value '$symbolName'")
                gValue
            } //TODO
            else -> throw ParseErrorException("constant or value", token)
        }
    }

    private fun getConstant(token: AnyValueToken, ty: Type): Value {
        return token.let {
            when (it) {
                is IntValue              -> Constant.of(ty, it.int)
                is FloatValue            -> Constant.of(ty, it.fp)
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

    fun begin(): Block {
        return blocks.begin()
    }

    fun build(): FunctionData {
        for (phi in incomplitedPhi) {
            phi.completePhi(nameMap)
        }

        return FunctionData.create(prototype, blocks, argumentValues)
    }

    fun makePrototype(functionName: SymbolValue, returnType: ElementaryTypeToken, argTypes: List<ElementaryTypeToken>): FunctionPrototype {
        val types = argTypes.mapTo(arrayListOf()) { it.type() }
        return FunctionPrototype(functionName.name, returnType.type(), types)
    }

    fun createLabel(): Block = allocateBlock()

    fun switchLabel(labelTok: LabelDefinition) {
        val label = getBlockOrCreate(labelTok.name)
        bb = blocks.findBlock(label)
    }

    fun arguments(): List<ArgumentValue> {
        return argumentValues
    }

    fun neg(name: LocalValueToken, valueTok: AnyValueToken, expectedType: ElementaryTypeToken): ArithmeticUnary {
        val value  = getValue(valueTok, expectedType.type())
        return memorize(name, bb.neg(value))
    }

    fun not(name: LocalValueToken, valueTok: AnyValueToken, expectedType: ElementaryTypeToken): ArithmeticUnary {
        val value  = getValue(valueTok, expectedType.type())
        return memorize(name, bb.not(value))
    }

    fun arithmeticBinary(name: LocalValueToken, a: AnyValueToken, op: ArithmeticBinaryOp, b: AnyValueToken, expectedType: ElementaryTypeToken): ArithmeticBinary {
        val first  = getValue(a, expectedType.type())
        val second = getValue(b, expectedType.type())
        val result = bb.arithmeticBinary(first, op, second)
        return memorize(name, result)
    }

    fun intCompare(name: LocalValueToken, a: AnyValueToken, predicate: Identifier, b: AnyValueToken, expectedType: ElementaryTypeToken): IntCompare {
        val compareType = when (predicate.string) {
            "eq"  -> IntPredicate.Eq
            "ne"  -> IntPredicate.Ne
            "uge" -> IntPredicate.Uge
            "ugt" -> IntPredicate.Ugt
            "ult" -> IntPredicate.Ult
            "ule" -> IntPredicate.Ule
            "sgt" -> IntPredicate.Sgt
            "sge" -> IntPredicate.Sge
            "slt" -> IntPredicate.Slt
            "sle" -> IntPredicate.Sle
            else  -> throw ParseErrorException("${predicate.position()} unknown compare type: cmpType=${predicate.string}")
        }

        val first  = getValue(a, expectedType.type())
        val second = getValue(b, expectedType.type())
        val result = bb.intCompare(first, compareType, second)
        return memorize(name, result)
    }

    fun floatCompare(name: LocalValueToken, a: AnyValueToken, predicate: Identifier, b: AnyValueToken, expectedType: ElementaryTypeToken): FloatCompare {
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

        val first  = getValue(a, expectedType.type())
        val second = getValue(b, expectedType.type())
        val result = bb.floatCompare(first, compareType, second)
        return memorize(name, result)
    }

    fun load(name: LocalValueToken, ptr: AnyValueToken, expectedType: ElementaryTypeToken): Load {
        val pointer = getValue(ptr, expectedType.type().ptr())
        return memorize(name, bb.load(expectedType.asType<PrimitiveType>(),pointer))
    }

    fun store(ptr: AnyValueToken, valueTok: AnyValueToken, expectedType: ElementaryTypeToken) {
        val pointer = getValue(ptr, expectedType.type())
        val value   = getValue(valueTok, expectedType.asType<PrimitiveType>())
        return bb.store(pointer, value)
    }

    fun call(name: LocalValueToken, func: AnyFunctionPrototype, args: ArrayList<AnyValueToken>): Value {
        require(func.type() !is VoidType)
        val argumentValues = arrayListOf<Value>()

        for ((arg, ty) in args zip func.arguments()) {
            argumentValues.add(getValue(arg, ty))
        }

        return memorize(name, bb.call(func, argumentValues))
    }

    fun vcall(func: AnyFunctionPrototype, args: ArrayList<AnyValueToken>) {
        require(func.type() is VoidType)
        val argumentValues = arrayListOf<Value>()

        for ((arg, ty) in args zip func.arguments()) {
            argumentValues.add(getValue(arg, ty))
        }

        bb.vcall(func, argumentValues)
    }

    private fun getBlockOrCreate(name: String): Block {
        val target = nameToLabel[name]
        return if (target == null) {
            val new = createLabel()
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

    fun branchCond(valueTok: LocalValueToken, onTrueName: LabelUsage, onFalseName: LabelUsage) {
        val onTrue  = getBlockOrCreate(onTrueName.labelName)
        val onFalse = getBlockOrCreate(onFalseName.labelName)

        val value = getValue(valueTok, Type.U1)
        bb.branchCond(value, onTrue, onFalse)
    }

    fun stackAlloc(name: LocalValueToken, ty: TypeToken): Alloc {
        return memorize(name, bb.alloc(ty.type()))
    }

    fun ret(retValue: AnyValueToken, expectedType: ElementaryTypeToken) {
        val value = getValue(retValue, expectedType.type())
        bb.ret(value)
    }

    fun retVoid() {
        bb.retVoid()
    }

    fun gep(name: LocalValueToken, type: ElementaryTypeToken, sourceName: AnyValueToken, sourceType: ElementaryTypeToken, indexName: AnyValueToken, indexType: ElementaryTypeToken): GetElementPtr {
        val source = getValue(sourceName, sourceType.type())
        val index  = getValue(indexName, indexType.type())
        return memorize(name, bb.gep(source, type.asType<PrimitiveType>(), index))
    }

    fun bitcast(name: LocalValueToken, operandToken: AnyValueToken, operandType: ElementaryTypeToken, resultType: ElementaryTypeToken): Bitcast {
        val value = getValue(operandToken, operandType.type())
        return memorize(name, bb.bitcast(value, resultType.asType<PrimitiveType>()))
    }

    fun zext(name: LocalValueToken, operandToken: AnyValueToken, operandType: ElementaryTypeToken, resultType: ElementaryTypeToken): ZeroExtend {
        val value = getValue(operandToken, operandType.type())
        return memorize(name, bb.zext(value, resultType.asType<IntegerType>()))
    }

    fun sext(name: LocalValueToken, operandToken: AnyValueToken, operandType: ElementaryTypeToken, resultType: ElementaryTypeToken): SignExtend {
        val value = getValue(operandToken, operandType.type())
        return memorize(name, bb.sext(value, resultType.asType<IntegerType>()))
    }

    fun trunc(name: LocalValueToken, operandToken: AnyValueToken, operandType: ElementaryTypeToken, resultType: ElementaryTypeToken): Truncate {
        val value = getValue(operandToken, operandType.type())
        return memorize(name, bb.trunc(value, resultType.asType<IntegerType>()))
    }

    fun fptrunc(name: LocalValueToken, operandToken: AnyValueToken, operandType: ElementaryTypeToken, resultType: ElementaryTypeToken): FpTruncate {
        val value = getValue(operandToken, operandType.type())
        return memorize(name, bb.fptrunc(value, resultType.asType<FloatingPointType>()))
    }

    fun fpext(name: LocalValueToken, operandToken: AnyValueToken, operandType: ElementaryTypeToken, resultType: ElementaryTypeToken): FpExtend {
        val value = getValue(operandToken, operandType.type())
        return memorize(name, bb.fpext(value, resultType.asType<FloatingPointType>()))
    }

    fun select(name: LocalValueToken, condTok: AnyValueToken, onTrueTok: AnyValueToken, onFalseTok: AnyValueToken, selectType: PrimitiveType): Value {
        val cond    = getValue(condTok, Type.U1)
        val onTrue  = getValue(onTrueTok, selectType)
        val onFalse = getValue(onFalseTok, selectType)

        return memorize(name, bb.select(cond, onTrue, onFalse))
    }

    fun phi(name: LocalValueToken, incomingTok: ArrayList<AnyValueToken>, labelsTok: ArrayList<Identifier>, expectedType: ElementaryTypeToken): Value {
        val blocks = arrayListOf<Block>()
        for (tok in labelsTok) {
            blocks.add(getBlockOrCreate(tok.string))
        }

        val type = expectedType.asType<PrimitiveType>()
        val values = arrayListOf<Value>()
        for (tok in incomingTok) {
            values.add(getConstant(tok, type))
        }

        val phi = bb.uncompletedPhi(values, blocks)
        incomplitedPhi.add(PhiContext(phi, incomingTok, type))
        return memorize(name, phi)
    }

    companion object {
        fun create(functionName: SymbolValue,
                   returnType: ElementaryTypeToken,
                   argumentTypeTokens: List<ElementaryTypeToken>,
                   argumentValueTokens: List<LocalValueToken>,
                   globals: Set<GlobalValue>): FunctionDataBuilderWithContext {
            fun handleArguments(argumentTypeTokens: List<ElementaryTypeToken>): List<ArgumentValue> {
                val argumentValues = arrayListOf<ArgumentValue>()
                for ((idx, arg) in argumentTypeTokens.withIndex()) {
                    argumentValues.add(ArgumentValue(idx, arg.type()))
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

            val args        = argumentTypeTokens.mapTo(arrayListOf()) { it.type() }
            val prototype   = FunctionPrototype(functionName.name, returnType.type(), args)
            val startBB     = Block.empty(Label.entry.index)
            val basicBlocks = BasicBlocks.create(startBB)

            val arguments = handleArguments(argumentTypeTokens)
            val nameMap   = setupNameMap(arguments, argumentValueTokens)

            return FunctionDataBuilderWithContext(prototype, arguments, basicBlocks, nameMap, globals)
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