package ir.read

import ir.*
import ir.block.Block
import ir.builder.FunctionDataBuilder
import ir.builder.ModuleBuilder
import ir.instruction.ArithmeticBinaryOp
import ir.instruction.CastType
import ir.instruction.IntPredicate
import ir.pass.ana.VerifySSA

class ParseErrorException(message: String): Exception(message) {
    constructor(expect: String, token: Token):
            this( "${token.position()} found: ${token.message()}, but expect $expect")
}

private class FunctionBlockReader(private val iterator: TokenIterator, private val builder: FunctionDataBuilder,
                                  private val nameToValue: MutableMap<String, Value>) {
    private val nameToLabel = hashMapOf("entry" to builder.begin())

    init {
        for (arg in builder.arguments()) {
            nameToValue[arg.name()] = arg
        }
    }

    private fun parseOperand(kind: TypeKind, errorMessage: String): Value {
        return parseOperand(iterator.next(errorMessage), kind)
    }

    private fun parseOperand(token: Token, kind: TypeKind): Value {
        return token.let {
            when (it) {
                is IntValue   -> Constant.of(kind, it.int)
                is FloatValue -> Constant.of(kind, it.fp)
                is ValueToken -> nameToValue[it.name] ?:
                    throw RuntimeException("in ${it.position()} undefined value")
                else -> throw ParseErrorException("constant or value", it)
            }
        }
    }

    private fun parseBinary(resultName: String, op: ArithmeticBinaryOp) {
        val resultType = iterator.expect<TypeToken>("result type")
        val first      = parseOperand(resultType.kind(), "first operand")
        iterator.expect<Comma>("','")

        val second = parseOperand(resultType.kind(), "second operand")
        nameToValue[resultName] = builder.arithmeticBinary(first, op, second)
    }

    private fun parseLoad(valueToken: ValueToken) {
        val typeToken    = iterator.expect<TypeToken>("loaded type")
        val pointerToken = iterator.expect<ValueToken>("pointer")
        val pointer      = nameToValue[pointerToken.name] ?:
            throw RuntimeException("in ${pointerToken.position()} undefined value")

        val loadedType   = typeToken.type()
        val actualType   = pointer.type().dereference()
        if (loadedType != actualType) {
            throw ParseErrorException("${valueToken.position()} incorrect type in load instruction: expect=$loadedType, actual=$actualType")
        }
        nameToValue[valueToken.name] = builder.load(pointer)
    }

    private fun parseStackAlloc(resultName: String) {
        val typeToken      = iterator.expect<TypeToken>("loaded type")
        val allocationSize = iterator.expect<IntValue>("stackalloc size")

        nameToValue[resultName] = builder.stackAlloc(typeToken.type().dereference(), allocationSize.int)
    }

    private fun parseStore(currentTok: Token) {
        val type         = iterator.expect<TypeToken>("stored value type")
        val pointerToken = iterator.expect<ValueToken>("pointer")
        val pointer      = nameToValue[pointerToken.name] ?:
        throw ParseErrorException("existed value", pointerToken)

        if (type.type() != pointer.type()) {
            throw ParseErrorException("${currentTok.position()} incorrect type: expect=${pointer.type()}, actual=${type.type()}")
        }
        iterator.expect<Comma>("','")
        val valueToken = parseOperand(type.kind(), "stored value")
        builder.store(pointer, valueToken)
    }

    private fun parseRet(currentTok: Token) {
        val retType = iterator.expect<TypeToken>("return type").type()
        val returnValue = parseOperand(retType.kind, "value or literal")

        val actualRetType = returnValue.type()
        if (retType != actualRetType) {
            throw ParseErrorException("${currentTok.position()} incorrect type: expect=${retType}, actual=${actualRetType}")
        }

        builder.ret(returnValue)
    }

    private fun parseCall(currentTok: ValueToken?) {
        val functionReturnType = iterator.expect<TypeToken>("function type").type()
        val functionName = iterator.expect<Identifier>("function name")

        iterator.expect<OpenParen>("'('")
        val argumentsType = arrayListOf<Type>()
        val argumentValue = arrayListOf<Value>()

        var valueToken = iterator.next("value")
        while (valueToken !is CloseParen) {

            iterator.expect<Colon>("':'")
            val type = iterator.expect<TypeToken>("argument type").type()

            val value = parseOperand(valueToken, type.kind)
            argumentValue.add(value)
            argumentsType.add(type)

            val comma = iterator.next("','")

            if (comma is CloseParen) {
                break
            }

            if (comma !is Comma) {
                throw ParseErrorException("type ", comma)
            }
            valueToken = iterator.next("value")
        }

        val prototype = FunctionPrototype(functionName.string, functionReturnType, argumentsType)

        if (currentTok != null) {
            nameToValue[currentTok.name] = builder.call(prototype, argumentValue)
        } else {
            builder.call(prototype, argumentValue)
        }
    }

    private fun parseCast(currentTok: ValueToken, castType: CastType) {
        val castToken = iterator.expect<TypeToken>("cast type")
        val cast = castToken.type()
        val castValueToken = iterator.expect<ValueToken>("cast value")
        val castValue = nameToValue[castValueToken.name]
            ?: throw ParseErrorException("existed value", castValueToken)

        nameToValue[currentTok.name] = builder.cast(castValue, cast, castType)
    }

    private fun parseCmp(resultTypeToken: ValueToken) {
        val compareTypeToken = iterator.expect<Identifier>("compare type")
        val compareType = when (compareTypeToken.string) {
            "eq" -> IntPredicate.Eq
            "ne" -> IntPredicate.Ne
            "uge" -> IntPredicate.Uge
            "ugt" -> IntPredicate.Ugt
            "ult" -> IntPredicate.Ult
            "ule" -> IntPredicate.Ule
            "sgt" -> IntPredicate.Sgt
            "sge" -> IntPredicate.Sge
            "slt" -> IntPredicate.Slt
            "sle" -> IntPredicate.Sle
            else  -> throw ParseErrorException("${compareTypeToken.position()} unknown compare type: cmpType=${compareTypeToken.string}")
        }

        val resultType = iterator.expect<TypeToken>("result type")
        val first = parseOperand(resultType.kind(), "first operand")
        iterator.expect<Comma>("','")

        val second = parseOperand(resultType.kind(), "second operand")
        nameToValue[resultTypeToken.name] = builder.intCompare(first, compareType, second)
    }

    private fun parsePhi(resultTypeToken: ValueToken) {
        val type = iterator.expect<TypeToken>("operands type").type()

        iterator.expect<OpenSquareBracket>("'['")
        val labels = arrayListOf<Block>()
        val argumentValue = arrayListOf<Value>()

        do {
            val value = parseOperand(type.kind, "value")
            iterator.expect<Colon>("':'")
            val labelToken = iterator.expect<Identifier>("label type")

            argumentValue.add(value)
            val label = nameToLabel.getOrPut(labelToken.string) {
                builder.createLabel()
            }
            labels.add(label)

            val comma = iterator.next("',' or ']'")
            if (comma is CloseSquareBracket) {
                break
            }

            if (comma !is Comma) {
                throw ParseErrorException("type ", comma)
            }
        } while (true)

        nameToValue[resultTypeToken.name] = builder.phi(argumentValue, labels)
    }

    private fun parseBranch() {
        val labelOrType = iterator.next("'label' or type")
        if (labelOrType is Identifier) {
            if (labelOrType.string == "label") {
                val labelNameToken = iterator.expect<Identifier>("label name")
                val labelName = nameToLabel.getOrPut(labelNameToken.string) {
                    builder.createLabel()
                }

                builder.branch(labelName)
            } else {
                throw ParseErrorException("'label'", labelOrType)
            }
        } else if (labelOrType is TypeToken) {
            val type = labelOrType.kind()
            val cmpValue = parseOperand(type, "cmp value")
            if (iterator.expect<Identifier>("'label'").string != "label") {
                throw ParseErrorException("label name", labelOrType)
            }

            val label1NameToken = iterator.expect<Identifier>("label name")
            val trueLabel = nameToLabel.getOrPut(label1NameToken.string) {
                builder.createLabel()
            }

            iterator.expect<Comma>("','")
            if (iterator.expect<Identifier>("'label'").string != "label") {
                throw ParseErrorException("label name", labelOrType)
            }

            val label2NameToken = iterator.expect<Identifier>("label name")
            val labelFalse = nameToLabel.getOrPut(label2NameToken.string) {
                builder.createLabel()
            }

            builder.branchCond(cmpValue, trueLabel, labelFalse)
        } else {
            throw ParseErrorException("'label' or type", labelOrType)
        }
    }

    private fun parseInstruction(currentTok: Token) {
        when (currentTok) {
            is ValueToken -> {
                iterator.expect<Equal>("'='")

                val instruction = iterator.expect<Identifier>("instruction name")
                when (instruction.string) {
                    "add"        -> parseBinary(currentTok.name, ArithmeticBinaryOp.Add)
                    "sub"        -> parseBinary(currentTok.name, ArithmeticBinaryOp.Sub)
                    "mul"        -> parseBinary(currentTok.name, ArithmeticBinaryOp.Mul)
                    "div"        -> parseBinary(currentTok.name, ArithmeticBinaryOp.Div)
                    "mod"        -> parseBinary(currentTok.name, ArithmeticBinaryOp.Mod)
                    "shr"        -> parseBinary(currentTok.name, ArithmeticBinaryOp.Shr)
                    "shl"        -> parseBinary(currentTok.name, ArithmeticBinaryOp.Shl)
                    "and"        -> parseBinary(currentTok.name, ArithmeticBinaryOp.And)
                    "xor"        -> parseBinary(currentTok.name, ArithmeticBinaryOp.Xor)
                    "or"         -> parseBinary(currentTok.name, ArithmeticBinaryOp.Or)
                    "load"       -> parseLoad(currentTok)
                    "call"       -> parseCall(currentTok)
                    "sext"       -> parseCast(currentTok, CastType.SignExtend)
                    "zext"       -> parseCast(currentTok, CastType.ZeroExtend)
                    "trunc"      -> parseCast(currentTok, CastType.Truncate)
                    "bitcast"    -> parseCast(currentTok, CastType.Bitcast)
                    "stackalloc" -> parseStackAlloc(currentTok.name)
                    "icmp"       -> parseCmp(currentTok)
                    "phi"        -> parsePhi(currentTok)
                    else -> throw ParseErrorException("instruction name", instruction)
                }
            }

            is LabelToken -> {
                val label = if (currentTok.name != "entry") {
                    nameToLabel.getOrPut(currentTok.name) {
                        builder.createLabel()
                    }
                } else {
                    builder.begin()
                }
                builder.switchLabel(label)
            }

            is Identifier -> {
                when (currentTok.string) {
                    "ret"   -> parseRet(currentTok)
                    "call"  -> parseCall(null)
                    "store" -> parseStore(currentTok)
                    "br"    -> parseBranch()
                    else    -> throw ParseErrorException("instruction", currentTok)
                }
            }

            else -> throw ParseErrorException("instruction", currentTok)
        }
    }

    private fun parseInstructions() {
        iterator.expect<OpenBrace>("'{'")
        var currentTok = iterator.next()
        while (currentTok !is CloseBrace) {
            parseInstruction(currentTok)
            currentTok = iterator.next()
        }
    }

    companion object {
        fun parse(tokenIterator: TokenIterator, builder: FunctionDataBuilder, nameToValue: MutableMap<String, Value>) {
            FunctionBlockReader(tokenIterator, builder, nameToValue).parseInstructions()
        }
    }
}

class ModuleReader(string: String) {
    private val tokenIterator = Tokenizer(string).iterator()
    private val moduleBuilder = ModuleBuilder()

    private fun parseModule() {
        if (!tokenIterator.hasNext()) {
            return
        }

        var tok = tokenIterator.next()
        while (tok is Extern) {
            parseExtern()
            if (!tokenIterator.hasNext()) {
                break
            }

            tok = tokenIterator.next()
        }

        while (tok is Define) {
            parseFunction()
            if (!tokenIterator.hasNext()) {
                break
            }

            tok = tokenIterator.next()
        }
    }

    private fun parseFunctionName(): String {
        return tokenIterator.next("function name").let {
            if (it !is Identifier) {
                throw ParseErrorException("function name", it)
            }

            it.string
        }
    }

    private fun parseExtern() {
        //extern <returnType> <function name> ( <type1>, <type2>, ...)
        val returnType = tokenIterator.expect<TypeToken>("return type").type()
        val functionName = parseFunctionName()

        tokenIterator.expect<OpenParen>("'('")

        val argumentsType = arrayListOf<Type>()
        do {
            val type = tokenIterator.expect<TypeToken>("argument type").type()
            argumentsType.add(type)

            val comma = tokenIterator.next("','")
            if (comma is CloseParen) {
                break
            }

            if (comma !is Comma) {
                throw ParseErrorException("type ", comma)
            }
        } while (true)

        moduleBuilder.createExternFunction(functionName, returnType, argumentsType)
    }

    private fun parseFunction() {
        // define <returnType> <functionName>(<value1>:<type1>, <value2>:<type2>,...)
        val nameToValue = hashMapOf<String, Value>()
        val returnType = tokenIterator.expect<TypeToken>("return type").type()
        val functionName = parseFunctionName()

        tokenIterator.expect<OpenParen>("'('")
        val argumentsType = arrayListOf<Type>()
        val argumentValue = arrayListOf<ArgumentValue>()

        var argumentNumber = 0
        do {
            val value = tokenIterator.next("value")
            if (value is CloseParen) {
                break
            }
            if (value !is ValueToken) {
                throw ParseErrorException("value ", value)
            }

            tokenIterator.expect<Colon>("':'")
            val type = tokenIterator.expect<TypeToken>("argument type").type()

            val arg = ArgumentValue(argumentNumber, type)
            argumentNumber += 1
            nameToValue[value.name] = arg
            argumentValue.add(arg)
            argumentsType.add(type)

            val comma = tokenIterator.next("','")
            if (comma is CloseParen) {
                break
            }

            if (comma !is Comma) {
                throw ParseErrorException("type ", comma)
            }
        } while (true)

        val fn = moduleBuilder.createFunction(functionName, returnType, argumentsType, argumentValue)
        FunctionBlockReader.parse(tokenIterator, fn, nameToValue)
    }

    fun read(): Module {
        parseModule()
        return VerifySSA.run(moduleBuilder.build())
    }
}