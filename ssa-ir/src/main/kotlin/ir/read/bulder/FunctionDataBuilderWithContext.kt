package ir.read.bulder

import ir.*
import ir.module.block.Block
import ir.module.block.Label
import ir.instruction.*
import ir.module.BasicBlocks
import ir.module.FunctionData
import ir.read.*
import ir.types.PointerType
import ir.types.PrimitiveType
import ir.types.Type
import ir.types.VoidType

class ParseErrorException(message: String): Exception(message) {
    constructor(expect: String, token: Token):
            this( "${token.position()} found: ${token.message()}, but expect $expect")
}

class FunctionDataBuilderWithContext private constructor(
    private val prototype: FunctionPrototype,
    private val argumentValues: List<ArgumentValue>,
    private val blocks: BasicBlocks,
    private val nameMap: MutableMap<String, LocalValue>
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

    private fun getValue(token: ValueToken, ty: Type): Value {
        return token.let {
            when (it) {
                is IntValue   -> Constant.of(ty, it.int)
                is FloatValue -> Constant.of(ty, it.fp)
                is ValueInstructionToken -> {
                    val operand = nameMap[it.name] ?:
                        throw RuntimeException("in ${it.position()} undefined value ${it.name}")

                    if (operand.type() != ty && operand.type() !is PointerType) {
                        throw ParseErrorException("must be the same type: in_file=$ty, find=${operand.type()} in ${token.position()}")
                    }

                    operand
                }
                else -> throw ParseErrorException("constant or value", it)
            }
        }
    }

    private fun getConstant(token: ValueToken, ty: Type): Value {
        return token.let {
            when (it) {
                is IntValue              -> Constant.of(ty, it.int)
                is FloatValue            -> Constant.of(ty, it.fp)
                is ValueInstructionToken -> Value.UNDEF
                else -> throw ParseErrorException("constant or value", it)
            }
        }
    }

    private inline fun<reified T: ValueInstruction> memorize(name: ValueInstructionToken, value: T): T {
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

    fun makePrototype(functionName: FunctionName, returnType: PrimitiveTypeToken, argTypes: List<PrimitiveTypeToken>): FunctionPrototype {
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

    fun arithmeticUnary(name: ValueInstructionToken, op: ArithmeticUnaryOp, valueTok: ValueToken, expectedType: PrimitiveTypeToken): ArithmeticUnary {
        val value  = getValue(valueTok, expectedType.type())
        return memorize(name, bb.arithmeticUnary(op, value))
    }

    fun arithmeticBinary(name: ValueInstructionToken, a: ValueToken, op: ArithmeticBinaryOp, b: ValueToken, expectedType: PrimitiveTypeToken): ArithmeticBinary {
        val first  = getValue(a, expectedType.type())
        val second = getValue(b, expectedType.type())
        val result = bb.arithmeticBinary(first, op, second)
        return memorize(name, result)
    }

    fun intCompare(name: ValueInstructionToken, a: ValueToken, predicate: Identifier, b: ValueToken, expectedType: PrimitiveTypeToken): IntCompare {
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

    fun load(name: ValueInstructionToken, ptr: ValueToken, expectedType: PrimitiveTypeToken): Load {
        val pointer = getValue(ptr, expectedType.type().ptr())
        return memorize(name, bb.load(pointer))
    }

    fun store(ptr: ValueToken, valueTok: ValueToken, expectedType: PrimitiveTypeToken) {
        val pointer = getValue(ptr, expectedType.type())
        val value   = getValue(valueTok, expectedType.asType<PointerType>().asDereference<PrimitiveType>())
        return bb.store(pointer, value)
    }

    fun call(name: ValueInstructionToken, func: AnyFunctionPrototype, args: ArrayList<ValueToken>): Value {
        require(func.type() !is VoidType)
        val argumentValues = arrayListOf<Value>()

        for ((arg, ty) in args zip func.arguments()) {
            argumentValues.add(getValue(arg, ty))
        }

        return memorize(name, bb.call(func, argumentValues))
    }

    fun vcall(func: AnyFunctionPrototype, args: ArrayList<ValueToken>) {
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

    fun branchCond(valueTok: ValueInstructionToken, onTrueName: LabelUsage, onFalseName: LabelUsage) {
        val onTrue  = getBlockOrCreate(onTrueName.labelName)
        val onFalse = getBlockOrCreate(onFalseName.labelName)

        val value = getValue(valueTok, Type.U1)
        bb.branchCond(value, onTrue, onFalse)
    }

    fun stackAlloc(name: ValueInstructionToken, ty: TypeToken): Alloc {
        return memorize(name, bb.alloc(ty.type()))
    }

    fun ret(retValue: ValueToken, expectedType: PrimitiveTypeToken) {
        val value = getValue(retValue, expectedType.type())
        bb.ret(value)
    }

    fun retVoid() {
        bb.retVoid()
    }

    fun gep(name: ValueInstructionToken, sourceName: ValueToken, sourceType: PrimitiveTypeToken, indexName: ValueToken, indexType: PrimitiveTypeToken): GetElementPtr {
        val source = getValue(sourceName, sourceType.type())
        val index  = getValue(indexName, indexType.type())
        return memorize(name, bb.gep(source, index))
    }

    fun cast(name: ValueInstructionToken, operandToken: ValueToken, operandType: PrimitiveTypeToken, cast: CastType, resultType: PrimitiveTypeToken): Cast {
        val value = getValue(operandToken, operandType.type())
        return memorize(name, bb.cast(value, resultType.type(), cast))
    }

    fun select(name: ValueInstructionToken, condTok: ValueToken, onTrueTok: ValueToken, onFalseTok: ValueToken, selectType: PrimitiveType): Value {
        val cond    = getValue(condTok, Type.U1)
        val onTrue  = getValue(onTrueTok, selectType)
        val onFalse = getValue(onFalseTok, selectType)

        return memorize(name, bb.select(cond, onTrue, onFalse))
    }

    fun phi(name: ValueInstructionToken, incomingTok: ArrayList<ValueToken>, labelsTok: ArrayList<Identifier>, expectedType: PrimitiveTypeToken): Value {
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
        fun create(functionName: FunctionName, returnType: PrimitiveTypeToken, argumentTypeTokens: List<PrimitiveTypeToken>, argumentValueTokens: List<ValueInstructionToken>): FunctionDataBuilderWithContext {
            fun handleArguments(argumentTypeTokens: List<PrimitiveTypeToken>): List<ArgumentValue> {
                val argumentValues = arrayListOf<ArgumentValue>()
                for ((idx, arg) in argumentTypeTokens.withIndex()) {
                    argumentValues.add(ArgumentValue(idx, arg.type()))
                }

                return argumentValues
            }

            fun setupNameMap(argument: List<ArgumentValue>, tokens: List<ValueInstructionToken>): MutableMap<String, LocalValue> {
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

            return FunctionDataBuilderWithContext(prototype, arguments, basicBlocks, nameMap)
        }
    }
}

private data class PhiContext(val phi: Phi, val valueTokens: List<ValueToken>, val expectedType: PrimitiveType) {
    fun completePhi(valueMap: Map<String, LocalValue>) {
        val values = phi.operands()
        for ((idx, tok) in valueTokens.withIndex()) {
            if (tok !is ValueInstructionToken) {
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