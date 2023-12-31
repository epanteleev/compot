package ir.read

import ir.*
import ir.types.*
import ir.module.Module
import ir.read.bulder.*
import ir.instruction.*


private class FunctionBlockReader private constructor(private val iterator: TokenIterator, private val builder: FunctionDataBuilderWithContext) {
    private fun parseOperand(expectMessage: String): AnyValueToken {
        return iterator.expect<AnyValueToken>(expectMessage)
    }

    private fun parseBinary(resultName: LocalValueToken, op: ArithmeticBinaryOp) {
        val resultType = iterator.expect<ElementaryTypeToken>("result type")
        val first      = parseOperand("first operand")
        iterator.expect<Comma>("','")

        val second = parseOperand("second operand")
        builder.arithmeticBinary(resultName, first, op, second, resultType)
    }

    private fun parseLoad(resultName: LocalValueToken) {
        val typeToken    = iterator.expect<ElementaryTypeToken>("loaded type")
        val pointerToken = iterator.expect<ValueToken>("pointer")

        builder.load(resultName, pointerToken, typeToken)
    }

    private fun parseStackAlloc(resultName: LocalValueToken) {
        val typeToken = iterator.expect<TypeToken>("loaded type")
        builder.stackAlloc(resultName, typeToken)
    }

    private fun parseStore() {
        iterator.expect<ElementaryTypeToken>("stored value type") //Todo check type
        val pointerToken = iterator.expect<ValueToken>("pointer")

        iterator.expect<Comma>("','")
        val valueTypeToken = iterator.expect<ElementaryTypeToken>("pointer")
        val valueToken = parseOperand("stored value")
        builder.store(pointerToken, valueToken, valueTypeToken)
    }

    private fun parseRet() {
        val retType     = iterator.expect<ElementaryTypeToken>("return type")
        if (retType.type() == Type.Void) {
            builder.retVoid()
            return
        }

        val returnValue = parseOperand("value or literal")
        builder.ret(returnValue, retType)
    }

    private fun parseCall(currentTok: LocalValueToken?) {
        val functionReturnType = iterator.expect<ElementaryTypeToken>("function type")
        val functionName       = iterator.expect<SymbolValue>("function name")
        val argumentsType      = arrayListOf<ElementaryTypeToken>()
        val argumentValue      = arrayListOf<AnyValueToken>()

        iterator.expect<OpenParen>("'('")
        var valueToken = iterator.next("value")
        while (valueToken !is CloseParen) {
            if (valueToken !is AnyValueToken) {
                throw ParseErrorException("value", valueToken)
            }

            iterator.expect<Colon>("':'")
            val type = iterator.expect<ElementaryTypeToken>("argument type")

            argumentValue.add(valueToken)
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

        val prototype = builder.makePrototype(functionName, functionReturnType, argumentsType)
        if (currentTok != null) {
            builder.call(currentTok, prototype, argumentValue)
        } else {
            builder.vcall(prototype, argumentValue)
        }
    }

    private fun parseZext(resultName: LocalValueToken) {
        val operandType = iterator.expect<ElementaryTypeToken>("value type")
        val operand     = iterator.expect<LocalValueToken>("cast value")
        iterator.expect<To>("'to' keyword")
        val castValueToken = iterator.expect<ElementaryTypeToken>("${ZeroExtend.NAME} type")

        builder.zext(resultName, operand, operandType, castValueToken)
    }

    private fun parseTrunc(resultName: LocalValueToken) {
        val operandType = iterator.expect<ElementaryTypeToken>("value type")
        val operand     = iterator.expect<LocalValueToken>("value to cast")
        iterator.expect<To>("'to' keyword")
        val castValueToken = iterator.expect<ElementaryTypeToken>("${Truncate.NAME} type")

        builder.trunc(resultName, operand, operandType, castValueToken)
    }

    private fun parseBitcast(resultName: LocalValueToken) {
        val operandType = iterator.expect<ElementaryTypeToken>("value type")
        val operand     = iterator.expect<LocalValueToken>("value to cast")
        iterator.expect<To>("'to' keyword")
        val castValueToken = iterator.expect<ElementaryTypeToken>("${Bitcast.NAME} type")

        builder.bitcast(resultName, operand, operandType, castValueToken)
    }

    private fun parseFptrunc(resultName: LocalValueToken) {
        val operandType = iterator.expect<ElementaryTypeToken>("value type")
        val operand     = iterator.expect<LocalValueToken>("value to cast")
        iterator.expect<To>("'to' keyword")
        val castValueToken = iterator.expect<ElementaryTypeToken>("${Fptruncate.NAME} type")

        builder.fptrunc(resultName, operand, operandType, castValueToken)
    }

    private fun parseFpext(resultName: LocalValueToken) {
        val operandType = iterator.expect<ElementaryTypeToken>("value type")
        val operand     = iterator.expect<LocalValueToken>("value to cast")
        iterator.expect<To>("'to' keyword")
        val castValueToken = iterator.expect<ElementaryTypeToken>("${FpExtend.NAME} type")

        builder.fpext(resultName, operand, operandType, castValueToken)
    }

    private fun parseSext(resultName: LocalValueToken) {
        val operandType = iterator.expect<ElementaryTypeToken>("value type")
        val operand     = iterator.expect<LocalValueToken>("value to cast")
        iterator.expect<To>("'to' keyword")
        val castValueToken = iterator.expect<ElementaryTypeToken>("${SignExtend.NAME} type")

        builder.sext(resultName, operand, operandType, castValueToken)
    }

    private fun parseIcmp(resultTypeToken: LocalValueToken) {
        val compareTypeToken = iterator.expect<Identifier>("compare type")
        val resultType       = iterator.expect<ElementaryTypeToken>("result type")
        val first            = iterator.expect<AnyValueToken>("compare operand")
        iterator.expect<Comma>("','")
        val second           = iterator.expect<AnyValueToken>("compare operand")

        if (resultType.type() !is IntegerType) {
            throw ParseErrorException("expect integer type, but resultType=$resultType")
        }

        builder.intCompare(resultTypeToken, first, compareTypeToken, second, resultType)
    }

    private fun parseFcmp(resultTypeToken: LocalValueToken) {
        val compareTypeToken = iterator.expect<Identifier>("compare type")
        val resultType       = iterator.expect<ElementaryTypeToken>("result type")
        val first            = iterator.expect<AnyValueToken>("compare operand")
        iterator.expect<Comma>("','")
        val second           = iterator.expect<AnyValueToken>("compare operand")

        if (resultType.type() !is FloatingPointType) {
            throw ParseErrorException("expect integer type, but resultType=$resultType")
        }

        builder.floatCompare(resultTypeToken, first, compareTypeToken, second, resultType)
    }

    private fun parsePhi(resultTypeToken: LocalValueToken) {
        val type = iterator.expect<ElementaryTypeToken>("operands type")

        iterator.expect<OpenSquareBracket>("'['")
        val labels = arrayListOf<Identifier>()
        val argumentValue = arrayListOf<AnyValueToken>()

        do {
            val value = iterator.expect<AnyValueToken>("value")
            iterator.expect<Colon>("':'")
            val labelToken = iterator.expect<Identifier>("label type")

            argumentValue.add(value)
            labels.add(labelToken)

            val comma = iterator.next("',' or ']'")
            if (comma is CloseSquareBracket) {
                break
            }

            if (comma !is Comma) {
                throw ParseErrorException("type ", comma)
            }
        } while (true)

        builder.phi(resultTypeToken, argumentValue, labels, type)
    }

    private fun parseBranch() {
        when (val labelOrType = iterator.next("'label' or type")) {
            is LabelUsage -> {
                builder.branch(labelOrType)
            }

            is ElementaryTypeToken -> {
                // br {cmpValue} label {trueLabel}, label {falseLabel}
                val cmpValue = iterator.expect<LocalValueToken>("value type")

                val trueLabel = iterator.expect<LabelUsage>("'label' with name")
                iterator.expect<Comma>("','")

                val labelFalse = iterator.expect<LabelUsage>("'label' with name")
                builder.branchCond(cmpValue, trueLabel, labelFalse)
            }

            else -> {
                throw ParseErrorException("'label' or type", labelOrType)
            }
        }
    }

    private fun parseGep(resultName: LocalValueToken) {
        //%$identifier = gep $type, ${source.type} {source}, ${index.type} ${index}
        val type = iterator.expect<ElementaryTypeToken>("type")
        iterator.expect<Comma>("comma")

        val sourceType = iterator.expect<ElementaryTypeToken>("type")
        val source     = iterator.expect<LocalValueToken>("source value")
        iterator.expect<Comma>("comma")
        val indexType  = iterator.expect<ElementaryTypeToken>("index type")
        val index      = iterator.expect<AnyValueToken>("index")

        builder.gep(resultName, type, source, sourceType, index, indexType)
    }

    private fun parseNeg(currentTok: LocalValueToken) {
        // %$identifier = {unary type name} {operand type} %{value}
        val type   = iterator.expect<ElementaryTypeToken>("type")
        val source = iterator.expect<LocalValueToken>("source value")
        builder.neg(currentTok, source, type)
    }

    private fun parseNot(currentTok: LocalValueToken) {
        // %$identifier = {unary type name} {operand type} %{value}
        val type   = iterator.expect<ElementaryTypeToken>("type")
        val source = iterator.expect<LocalValueToken>("source value")
        builder.neg(currentTok, source, type)
    }

    private fun parseInstruction(currentTok: Token) {
        when (currentTok) {
            is LocalValueToken -> {
                iterator.expect<Equal>("'='")

                val instruction = iterator.expect<Identifier>("instruction name")
                when (instruction.string) {
                    "add"        -> parseBinary(currentTok, ArithmeticBinaryOp.Add)
                    "sub"        -> parseBinary(currentTok, ArithmeticBinaryOp.Sub)
                    "mul"        -> parseBinary(currentTok, ArithmeticBinaryOp.Mul)
                    "div"        -> parseBinary(currentTok, ArithmeticBinaryOp.Div)
                    "mod"        -> parseBinary(currentTok, ArithmeticBinaryOp.Mod)
                    "shr"        -> parseBinary(currentTok, ArithmeticBinaryOp.Shr)
                    "shl"        -> parseBinary(currentTok, ArithmeticBinaryOp.Shl)
                    "and"        -> parseBinary(currentTok, ArithmeticBinaryOp.And)
                    "xor"        -> parseBinary(currentTok, ArithmeticBinaryOp.Xor)
                    "or"         -> parseBinary(currentTok, ArithmeticBinaryOp.Or)
                    "load"       -> parseLoad(currentTok)
                    "call"       -> parseCall(currentTok)
                    "sext"       -> parseSext(currentTok)
                    "zext"       -> parseZext(currentTok)
                    "trunc"      -> parseTrunc(currentTok)
                    "bitcast"    -> parseBitcast(currentTok)
                    "fptrunc"    -> parseFptrunc(currentTok)
                    "fpext"      -> parseFpext(currentTok)
                    "alloc"      -> parseStackAlloc(currentTok)
                    "phi"        -> parsePhi(currentTok)
                    "gep"        -> parseGep(currentTok)
                    "neg"        -> parseNeg(currentTok)
                    "not"        -> parseNot(currentTok)
                    "icmp"       -> parseIcmp(currentTok)
                    "fcmp"       -> parseFcmp(currentTok)
                    else -> throw ParseErrorException("instruction name", instruction)
                }
            }

            is LabelDefinition -> {
                builder.switchLabel(currentTok)
            }

            is Identifier -> {
                when (currentTok.string) {
                    "ret"   -> parseRet()
                    "call"  -> parseCall(null)
                    "store" -> parseStore()
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
        fun parse(tokenIterator: TokenIterator, builder: FunctionDataBuilderWithContext) {
            FunctionBlockReader(tokenIterator, builder).parseInstructions()
        }
    }
}

class ModuleReader(string: String) {
    private val tokenIterator = Tokenizer(string).iterator()
    private val moduleBuilder = ModuleBuilderWithContext.create()

    private fun parseModule() {
        if (!tokenIterator.hasNext()) {
            return
        }

        do {
            when (val tok = tokenIterator.next()) {
                is Extern -> parseExtern()
                is Define -> parseFunction()
                is SymbolValue -> parseGlobals(tok)
                else -> throw ParseErrorException("function, extern or global constant", tok)
            }
        } while (tokenIterator.hasNext())
    }

    private fun parseGlobals(name: SymbolValue) {
        tokenIterator.expect<Equal>("'='")
        val keyword = tokenIterator.next("'constant' or 'global'")
        if (keyword !is GlobalKeyword && keyword !is ConstantKeyword) {
            throw ParseErrorException("'constant' or 'global'", keyword)
        }
        val type = tokenIterator.expect<TypeToken>("constant type")

        val global = when (val data = tokenIterator.next()) {
            is IntValue -> {
                when (val tp = type.type()) {
                    Type.I8 -> I8GlobalValue(name.name, data.int.toByte())
                    Type.U8 -> U8GlobalValue(name.name, data.int.toUByte())
                    Type.I16 -> I16GlobalValue(name.name, data.int.toShort())
                    Type.U16 -> U16GlobalValue(name.name, data.int.toUShort())
                    Type.I32 -> I32GlobalValue(name.name, data.int.toInt())
                    Type.U32 -> U32GlobalValue(name.name, data.int.toUInt())
                    Type.I64 -> I64GlobalValue(name.name, data.int)
                    Type.U64 -> U64GlobalValue(name.name, data.int.toULong())
                    else -> throw ParseErrorException("unsupported: type=$tp, data=${data.int}")
                }
            }
            is FloatValue -> {
                when (val tp = type.type()) {
                    Type.F32 -> F32GlobalValue(name.name, data.fp.toFloat())
                    Type.F64 -> F64GlobalValue(name.name, data.fp)
                    else -> throw ParseErrorException("unsupported: type=$tp, data=${data.fp}")
                }
            }
            is StringLiteralToken -> {
                StringLiteralGlobal(name.name, type.asType<ArrayType>(), data.string)
            }
            else -> throw ParseErrorException("unsupported: data=$data")
        }
        moduleBuilder.addGlobal(global)
    }

    private fun parseExtern() {
        //extern <returnType> <function name> ( <type1>, <type2>, ...)
        val returnType = tokenIterator.expect<ElementaryTypeToken>("return type")
        val functionName = tokenIterator.expect<SymbolValue>("function name")

        tokenIterator.expect<OpenParen>("'('")

        val argumentsType = arrayListOf<ElementaryTypeToken>()
        do {
            val type = tokenIterator.expect<ElementaryTypeToken>("argument type")
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
        val returnType = tokenIterator.expect<ElementaryTypeToken>("return type")
        val functionName = tokenIterator.expect<SymbolValue>("function name")

        tokenIterator.expect<OpenParen>("'('")
        val argumentsType = arrayListOf<ElementaryTypeToken>()
        val argumentValue = arrayListOf<LocalValueToken>()

        do {
            val value = tokenIterator.next("value")
            if (value is CloseParen) {
                break
            }
            if (value !is LocalValueToken) {
                throw ParseErrorException("value ", value)
            }

            tokenIterator.expect<Colon>("':'")
            val type = tokenIterator.expect<ElementaryTypeToken>("argument type")

            argumentValue.add(value)
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
        FunctionBlockReader.parse(tokenIterator, fn)
    }

    fun read(): Module {
        parseModule()
        return moduleBuilder.build()
    }
}