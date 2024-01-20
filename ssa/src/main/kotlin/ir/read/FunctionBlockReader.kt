package ir.read

import ir.types.*
import ir.instruction.*
import ir.read.bulder.*


class FunctionBlockReader private constructor(private val iterator: TokenIterator,
                                                      private val moduleBuilder: ModuleBuilderWithContext,
                                                      private val builder: FunctionDataBuilderWithContext
) {
    private fun parseOperand(expectMessage: String): AnyValueToken {
        return iterator.expect<AnyValueToken>(expectMessage)
    }

    private fun parseBinary(resultName: LocalValueToken, op: ArithmeticBinaryOp) {
        val resultType = iterator.expect<ArithmeticTypeToken>("result type")
        val first      = parseOperand("first operand")
        iterator.expect<Comma>("','")

        val second = parseOperand("second operand")
        builder.arithmeticBinary(resultName, first, op, second, resultType)
    }

    private fun parseLoad(resultName: LocalValueToken) {
        val typeToken    = iterator.expect<PrimitiveTypeToken>("loaded type")
        val pointerToken = iterator.expect<ValueToken>("pointer")

        builder.load(resultName, pointerToken, typeToken)
    }

    private fun parseStackAlloc(resultName: LocalValueToken) {
        val typeToken = iterator.expect<TypeToken>("loaded type")
        val type = typeToken.type(moduleBuilder)

        builder.alloc(resultName, type)
    }

    private fun parseStore() {
        iterator.expect<PrimitiveTypeToken>("stored value type")
        val pointerToken = iterator.expect<ValueToken>("pointer")

        iterator.expect<Comma>("','")
        val valueTypeToken = iterator.expect<PrimitiveTypeToken>("pointer")
        val valueToken = parseOperand("stored value")
        builder.store(pointerToken, valueToken, valueTypeToken)
    }

    private fun parseRet() {
        val retType = iterator.expect<TypeToken>("return type")
        if (retType.type(moduleBuilder) == Type.Void) {
            builder.retVoid()
            return
        }

        val returnValue = parseOperand("value or literal")
        builder.ret(returnValue, retType)
    }

    private fun tryParseArgumentBlock(argumentsTypes: MutableList<TypeToken>, argumentValues: MutableList<AnyValueToken>) {
        iterator.expect<OpenParen>("'('")
        var valueToken = iterator.next("value")
        while (valueToken !is CloseParen) {
            if (valueToken !is AnyValueToken) {
                throw ParseErrorException("value", valueToken)
            }

            iterator.expect<Colon>("':'")
            val type = iterator.expect<TypeToken>("argument type")

            argumentValues.add(valueToken)
            argumentsTypes.add(type)

            val comma = iterator.next("','")
            if (comma is CloseParen) {
                break
            }
            if (comma !is Comma) {
                throw ParseErrorException("type ", comma)
            }
            valueToken = iterator.next("value")
        }
    }

    private fun parseCall(currentTok: LocalValueToken) {
        val functionReturnType = iterator.expect<TypeToken>("function type")
        val functionName       = iterator.expect<SymbolValue>("function name")

        val argumentsTypes      = arrayListOf<TypeToken>()
        val argumentValues      = arrayListOf<AnyValueToken>()
        tryParseArgumentBlock(argumentsTypes, argumentValues)

        val prototype = builder.makePrototype(functionName, functionReturnType, argumentsTypes)
        builder.call(currentTok, prototype, argumentValues)
    }

    private fun parseVCall() {
        val functionReturnType = iterator.expect<TypeToken>("function type")
        val funcNameOrValue    = iterator.next("function name or value")

        val argumentsTypes      = arrayListOf<TypeToken>()
        val argumentValues      = arrayListOf<AnyValueToken>()
        tryParseArgumentBlock(argumentsTypes, argumentValues)

        when (funcNameOrValue) {
            is SymbolValue -> {
                val prototype = builder.makePrototype(funcNameOrValue, functionReturnType, argumentsTypes)
                builder.vcall(prototype, argumentValues)
            }
            is LocalValueToken -> {
                val prototype = builder.makePrototype(functionReturnType, argumentsTypes)
                builder.ivcall(funcNameOrValue, prototype, argumentValues)
            }
            else -> throw ParseErrorException("function name or value", funcNameOrValue)
        }
    }

    private fun parseZext(resultName: LocalValueToken) {
        val operandType = iterator.expect<IntegerTypeToken>("value type")
        val operand     = iterator.expect<LocalValueToken>("cast value")
        iterator.expect<To>("'to' keyword")
        val castValueToken = iterator.expect<IntegerTypeToken>("${ZeroExtend.NAME} type")

        builder.zext(resultName, operand, operandType, castValueToken)
    }

    private fun parseTrunc(resultName: LocalValueToken) {
        val operandType = iterator.expect<IntegerTypeToken>("value type")
        val operand     = iterator.expect<LocalValueToken>("value to cast")
        iterator.expect<To>("'to' keyword")
        val castValueToken = iterator.expect<IntegerTypeToken>("${Truncate.NAME} type")

        builder.trunc(resultName, operand, operandType, castValueToken)
    }

    private fun parseBitcast(resultName: LocalValueToken) {
        val operandType = iterator.expect<PrimitiveTypeToken>("value type")
        val operand     = iterator.expect<LocalValueToken>("value to cast")
        iterator.expect<To>("'to' keyword")
        val castValueToken = iterator.expect<PrimitiveTypeToken>("${Bitcast.NAME} type")

        builder.bitcast(resultName, operand, operandType, castValueToken)
    }

    private fun parseFptrunc(resultName: LocalValueToken) {
        val operandType = iterator.expect<FloatTypeToken>("value type")
        val operand     = iterator.expect<LocalValueToken>("value to cast")
        iterator.expect<To>("'to' keyword")
        val castValueToken = iterator.expect<FloatTypeToken>("${FpTruncate.NAME} type")

        builder.fptrunc(resultName, operand, operandType, castValueToken)
    }

    private fun parseFpext(resultName: LocalValueToken) {
        val operandType = iterator.expect<FloatTypeToken>("value type")
        val operand     = iterator.expect<LocalValueToken>("value to cast")
        iterator.expect<To>("'to' keyword")
        val castValueToken = iterator.expect<FloatTypeToken>("${FpExtend.NAME} type")

        builder.fpext(resultName, operand, operandType, castValueToken)
    }

    private fun parseFptosi(resultName: LocalValueToken) {
        val operandType = iterator.expect<FloatTypeToken>("value type")
        val operand     = iterator.expect<LocalValueToken>("value to cast")
        iterator.expect<To>("'to' keyword")
        val castValueToken = iterator.expect<SignedIntegerTypeToken>("${FloatToSigned.NAME} type")

        builder.fptosi(resultName, operand, operandType, castValueToken)
    }

    private fun parseSext(resultName: LocalValueToken) {
        val operandType = iterator.expect<IntegerTypeToken>("value type")
        val operand     = iterator.expect<LocalValueToken>("value to cast")
        iterator.expect<To>("'to' keyword")
        val castValueToken = iterator.expect<IntegerTypeToken>("${SignExtend.NAME} type")

        builder.sext(resultName, operand, operandType, castValueToken)
    }

    private fun parseIcmp(resultTypeToken: LocalValueToken) {
        val compareTypeToken = iterator.expect<Identifier>("compare type")
        val operandsTypes       = iterator.expect<SignedIntegerTypeToken>("signed integer operands type")
        val first            = iterator.expect<AnyValueToken>("first compare operand")
        iterator.expect<Comma>("','")
        val second           = iterator.expect<AnyValueToken>("second compare operand")

        builder.icmp(resultTypeToken, first, compareTypeToken, second, operandsTypes)
    }

    private fun parseUcmp(resultTypeToken: LocalValueToken) {
        val compareTypeToken = iterator.expect<Identifier>("compare type")
        val operandsTypes       = iterator.expect<UnsignedIntegerTypeToken>("unsigned integer operands type")
        val first            = iterator.expect<AnyValueToken>("first compare operand")
        iterator.expect<Comma>("','")
        val second           = iterator.expect<AnyValueToken>("second compare operand")

        builder.ucmp(resultTypeToken, first, compareTypeToken, second, operandsTypes)
    }

    private fun parseFcmp(resultTypeToken: LocalValueToken) {
        val compareTypeToken = iterator.expect<Identifier>("compare type")
        val operandsTypes       = iterator.expect<FloatTypeToken>("floating point operands type")
        val first            = iterator.expect<AnyValueToken>("first compare operand")
        iterator.expect<Comma>("','")
        val second           = iterator.expect<AnyValueToken>("second compare operand")

        builder.floatCompare(resultTypeToken, first, compareTypeToken, second, operandsTypes)
    }

    private fun parsePhi(resultTypeToken: LocalValueToken) {
        val type = iterator.expect<PrimitiveTypeToken>("operands type")

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

            is PrimitiveTypeToken -> {
                // br {cmpValue} label {trueLabel}, label {falseLabel}
                val cmpValue = iterator.expect<LocalValueToken>("value type")

                val trueLabel = iterator.expect<LabelUsage>("'label' with name")
                iterator.expect<Comma>("','")

                val labelFalse = iterator.expect<LabelUsage>("'label' with name")
                builder.branchCond(cmpValue, trueLabel, labelFalse)
            }

            else -> throw ParseErrorException("'label' or type", labelOrType)
        }
    }

    private fun parseGep(resultName: LocalValueToken) {
        //%$identifier = gep $type, ${source.type} {source}, ${index.type} ${index}
        val type = iterator.expect<PrimitiveTypeToken>("type")
        iterator.expect<Comma>("comma")

        val sourceType = iterator.expect<PointerTypeToken>("type")
        val source     = iterator.expect<ValueToken>("source value")
        iterator.expect<Comma>("comma")
        val indexType  = iterator.expect<IntegerTypeToken>("index type")
        val index      = iterator.expect<AnyValueToken>("index")

        builder.gep(resultName, type, source, sourceType, index, indexType)
    }

    private fun parseGfp(resultName: LocalValueToken) {
        //%$identifier = gfp $type, ${source.type} {source}, ${index.type} ${index}
        val type = iterator.expect<AggregateTypeToken>("type")
        iterator.expect<Comma>("comma")

        val sourceType = iterator.expect<PointerTypeToken>("type")
        val source     = iterator.expect<LocalValueToken>("source value")
        iterator.expect<Comma>("comma")
        val indexType  = iterator.expect<IntegerTypeToken>("index type")
        val index      = iterator.expect<IntValue>("index")

        builder.gfp(resultName, type, source, sourceType, index, indexType)
    }

    private fun parseNeg(currentTok: LocalValueToken) {
        // %$identifier = {unary type name} {operand type} %{value}
        val type   = iterator.expect<ArithmeticTypeToken>("type")
        val source = iterator.expect<LocalValueToken>("source value")
        builder.neg(currentTok, source, type)
    }

    private fun parseNot(currentTok: LocalValueToken) {
        // %$identifier = {unary type name} {operand type} %{value}
        val type   = iterator.expect<IntegerTypeToken>("type")
        val source = iterator.expect<LocalValueToken>("source value")
        builder.neg(currentTok, source, type)
    }

    private fun parseSelect(currentTok: LocalValueToken) {
        // %$identifier = select u1 <v0>, <t1> <v1>, <t2> <v2>
        iterator.expect<BooleanTypeToken>("'${Type.U1}' type")
        val v0 = iterator.expect<AnyValueToken>("condition value")
        iterator.expect<Comma>("','")

        val t1 = iterator.expect<PrimitiveTypeToken>("primitive type")
        val v1 = iterator.expect<AnyValueToken>("first operand")
        iterator.expect<Comma>("','")

        val t2 = iterator.expect<PrimitiveTypeToken>("primitive type")
        val v2 = iterator.expect<AnyValueToken>("second operand")

        if (t1.type() != t2.type()) {
            throw ParseErrorException("should be the same primitive type: t1=${t1.type()}, t2=${t2.type()}")
        }

        builder.select(currentTok, v0, v1, v2, t1)
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
                    "fptosi"     -> parseFptosi(currentTok)
                    "alloc"      -> parseStackAlloc(currentTok)
                    "phi"        -> parsePhi(currentTok)
                    "gep"        -> parseGep(currentTok)
                    "neg"        -> parseNeg(currentTok)
                    "not"        -> parseNot(currentTok)
                    "icmp"       -> parseIcmp(currentTok)
                    "ucmp"       -> parseUcmp(currentTok)
                    "fcmp"       -> parseFcmp(currentTok)
                    "gfp"        -> parseGfp(currentTok)
                    "select"     -> parseSelect(currentTok)
                    else -> throw ParseErrorException("instruction name", instruction)
                }
            }

            is LabelDefinition -> {
                builder.switchLabel(currentTok)
            }

            is Identifier -> {
                when (currentTok.string) {
                    "ret"   -> parseRet()
                    "call"  -> parseVCall()
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
        fun parse(
            tokenIterator: TokenIterator,
            moduleBuilder: ModuleBuilderWithContext,
            builder: FunctionDataBuilderWithContext
        ) {
            FunctionBlockReader(tokenIterator, moduleBuilder, builder).parseInstructions()
        }
    }
}