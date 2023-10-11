package ir.read.bulder

import ir.*
import ir.block.Block
import ir.block.Label
import ir.instruction.*
import ir.read.*

class FunctionDataBuilderWithContext private constructor(
    private val prototype: FunctionPrototype,
    private val argumentValues: List<ArgumentValue>,
    private val blocks: BasicBlocks,
    private val nameMap: MutableMap<String, LocalValue>
) {
    private var allocatedLabel: Int = 0
    private var bb: Block = blocks.begin()
    private val nameToLabel = hashMapOf<String, Block>()

    private fun allocateBlock(): Block {
        allocatedLabel += 1
        val bb = Block.empty(allocatedLabel)
        blocks.putBlock(bb)
        return bb
    }

    private fun getValue(token: Token, ty: Type): Value {
        return token.let {
            when (it) {
                is IntValue   -> Constant.of(ty.kind, it.int)
                is FloatValue -> Constant.of(ty.kind, it.fp)
                is ValueToken -> {
                    val operand = nameMap[it.name] ?:
                        throw RuntimeException("in ${it.position()} undefined value")

                    assert(operand.type() == ty) {
                        "must be the same type: in_file=$ty, find=${operand.type()}"
                    }

                    operand
                }
                else -> throw ParseErrorException("constant or value", it)
            }
        }
    }

    private inline fun<reified T: ValueInstruction> memorize(name: String, value: T): T {
        assert(nameMap[name] == null) {
            "already has value with the same name: nameMap[$name]=${nameMap[name]}"
        }

        nameMap[name] = value
        return value
    }

    fun begin(): Block {
        return blocks.begin()
    }

    fun build(): FunctionData {
        return FunctionData.create(prototype, blocks, argumentValues)
    }

    fun createLabel(): Block = allocateBlock()

    fun switchLabel(label: Label) {
        bb = blocks.findBlock(label)
    }

    fun arguments(): List<ArgumentValue> {
        return argumentValues
    }

    fun arithmeticUnary(name: String, op: ArithmeticUnaryOp, valueTok: ValueToken, expectedType: Type): ArithmeticUnary {
        val value  = getValue(valueTok, expectedType)
        val result = bb.arithmeticUnary(op, value)
        return memorize(name, result)
    }

    fun arithmeticBinary(name: String, a: ValueToken, op: ArithmeticBinaryOp, b: ValueToken, expectedType: Type): ArithmeticBinary {
        val first  = getValue(a, expectedType)
        val second = getValue(b, expectedType)
        val result = bb.arithmeticBinary(first, op, second)
        return memorize(name, result)
    }

    fun intCompare(name: String, a: ValueToken, pred: IntPredicate, b: ValueToken, expectedType: Type): IntCompare {
        val first  = getValue(a, expectedType)
        val second = getValue(b, expectedType)
        val result = bb.intCompare(first, pred, second)
        return memorize(name, result)
    }

    fun load(name: String, ptr: ValueToken, expectedType: Type): Load {
        val pointer = getValue(ptr, expectedType)
        return memorize(name, bb.load(pointer))
    }

    fun store(ptr: ValueToken, valueTok: ValueToken, expectedType: Type) {
        val pointer = getValue(ptr, expectedType)
        val value = getValue(valueTok, expectedType)
        return bb.store(pointer, value)
    }

    fun call(func: AnyFunctionPrototype, args: ArrayList<ValueToken>): Value {
        val argumentValues = arrayListOf<Value>()

        for ((arg, ty) in args zip func.arguments()) {
            argumentValues.add(getValue(arg, ty))
        }

        return bb.call(func, argumentValues)
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

    fun branch(targetName: LabelToken) {
        val block = getBlockOrCreate(targetName.name)
        bb.branch(block)
    }

    fun branchCond(value: Value, onTrueName: LabelToken, onFalseName: LabelToken) {
        val onTrue  = getBlockOrCreate(onTrueName.name)
        val onFalse = getBlockOrCreate(onFalseName.name)

        bb.branchCond(value, onTrue, onFalse)
    }

    fun stackAlloc(name: String, ty: Type, size: Long): StackAlloc {
        return memorize(name, bb.stackAlloc(ty, size))
    }

    fun ret(retValue: ValueToken, expectedType: Type) {
        val value = getValue(retValue, expectedType)
        bb.ret(value)
    }

    fun gep(name: String, sourceName: ValueToken, indexName: ValueToken, sourceType: Type): GetElementPtr {
        val source = getValue(sourceName, sourceType)
        val index  = getValue(indexName, Type.U64) //Todo bug
        return memorize(name, bb.gep(source, index))
    }

    fun cast(name: String, valueTok: ValueToken, ty: Type, cast: CastType, valueType: Type): Cast {
        val value = getValue(valueTok, valueType)
        return memorize(name, bb.cast(value, ty, cast))
    }

    fun select(name: String, condTok: ValueToken, onTrueTok: ValueToken, onFalseTok: ValueToken, selectType: Type): Value {
        val cond    = getValue(condTok, Type.U1)
        val onTrue  = getValue(onTrueTok, selectType)
        val onFalse = getValue(onFalseTok, selectType)

        return memorize(name, bb.select(cond, onTrue, onFalse))
    }

    fun phi(name: String, incomingTok: ArrayList<ValueToken>, labelsTok: ArrayList<LabelToken>, expectedType: Type): Value {
        //Todo incorrect impl
        val incoming = arrayListOf<Value>()
        for (tok in incomingTok) {
            incoming.add(getValue(tok, expectedType))
        }

        val blocks = arrayListOf<Block>()
        for (tok in labelsTok) {
            blocks.add(getBlockOrCreate(tok.name))
        }

        return memorize(name, bb.phi(incoming, blocks))
    }

    companion object {
        fun create(name: Identifier, returnType: TypeToken, argumentTypeTokens: List<TypeToken>, argumentValueTokens: List<ValueToken>): FunctionDataBuilderWithContext {
            fun handleArguments(argumentTypeTokens: List<TypeToken>): List<ArgumentValue> {
                val argumentValues = arrayListOf<ArgumentValue>()
                for ((idx, arg) in argumentTypeTokens.withIndex()) {
                    argumentValues.add(ArgumentValue(idx, arg.type()))
                }

                return argumentValues
            }

            fun setupNameMap(argument: List<ArgumentValue>, tokens: List<ValueToken>): MutableMap<String, LocalValue> {
                val nameToValue = hashMapOf<String, LocalValue>()
                for ((arg, tok) in argument zip tokens) {
                    nameToValue[tok.name] = arg
                }

                return nameToValue
            }

            val args = argumentTypeTokens.mapTo(arrayListOf()) { it.type() }
            val prototype = FunctionPrototype(name.string, returnType.type(), args)
            val startBB = Block.empty(Label.entry.index)
            val basicBlocks = BasicBlocks.create(startBB)

            val arguments = handleArguments(argumentTypeTokens)
            val nameMap = setupNameMap(arguments, argumentValueTokens)

            val builder = FunctionDataBuilderWithContext(prototype, arguments, basicBlocks, nameMap)
            builder.switchLabel(startBB)
            return builder
        }
    }
}