package ir.read

import ir.value.Constant
import ir.value.UnsignedIntegerConstant
import ir.types.*
import ir.instruction.*
import ir.read.bulder.*
import ir.read.tokens.*


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

    private fun parseDiv(resultName: LocalValueToken) {
        when (val resultType = iterator.expect<TypeToken>("result type")) {
            is TupleTypeToken -> {
                // %$resultName = div {tuple_type}, {firstType} {first}, {secondType} {second}
                iterator.expect<Comma>("','")
                val firstType = iterator.expect<IntegerTypeToken>("first operand type")
                val first = parseOperand("first operand")
                iterator.expect<Comma>("','")
                val secondType = iterator.expect<IntegerTypeToken>("second operand type")
                val second = parseOperand("second operand")
                if (firstType.type() != secondType.type()) {
                    throw ParseErrorException("should be the same integer type: first=${firstType.type()}, second=${secondType.type()}")
                }

                builder.tupleDiv(resultName, resultType, first, second, secondType)
            }
            else -> {
                iterator.expect<Comma>("','")
                val firstType = iterator.expect<PrimitiveTypeToken>("first operand type")
                val first = parseOperand("first operand")
                iterator.expect<Comma>("','")

                val secondType = iterator.expect<PrimitiveTypeToken>("second operand type")
                val second = parseOperand("second operand")

                if (firstType.type() != secondType.type()) {
                    throw ParseErrorException("should be the same integer type: first=${firstType.type()}, second=${secondType.type()}")
                }
                if (resultType !is ArithmeticTypeToken) {
                    throw ParseErrorException("should be arithmetic type, but '${resultType}'")
                }
                builder.arithmeticBinary(resultName, first, ArithmeticBinaryOp.Div, second, resultType)
            }
        }
    }

    private fun parseLoad(resultName: LocalValueToken) {
        val typeToken    = iterator.expect<PrimitiveTypeToken>("loaded type")
        val pointerToken = iterator.expect<ValueToken>("type '${Type.Ptr}'")

        builder.load(resultName, pointerToken, typeToken)
    }

    private fun parseStackAlloc(resultName: LocalValueToken) {
        val typeToken = iterator.expect<TypeToken>("loaded type")
        val type = typeToken.type(moduleBuilder)
        if (type !is NonTrivialType) {
            throw TypeErrorException("expected non-trivial type, but found type=${type}")
        }

        builder.alloc(resultName, type)
    }

    private fun parseStore() {
        iterator.expect<PrimitiveTypeToken>("stored value type")
        val pointerToken = iterator.expect<ValueToken>("pointer value")

        iterator.expect<Comma>("','")
        val valueTypeToken = iterator.expect<PrimitiveTypeToken>("value to store")
        val valueToken = parseOperand("stored value")
        builder.store(pointerToken, valueToken, valueTypeToken)
    }

    private fun parseRet() {
        val retType = iterator.expect<TypeToken>("return type")
        if (retType.type(moduleBuilder) == Type.Void) {
            builder.retVoid()
            return
        }

        val next = iterator.next("value or '{'")
        if (next is AnyValueToken) {
            builder.ret(arrayListOf(next), retType)
            return
        }

        if (next !is OpenBrace) {
            throw ParseErrorException("value or '{'", next)
        }
        val returnValues = arrayListOf<AnyValueToken>()
        do {
            val value = iterator.expect<AnyValueToken>("value")
            returnValues.add(value)

            val comma = iterator.next("',' or '}'")
            if (comma is CloseBrace) {
                break
            }

            if (comma !is Comma) {
                throw ParseErrorException("','", comma)
            }
        } while (true)
        builder.ret(returnValues, retType)
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
        val functionReturnType = iterator.expect<TypeToken>("function return type") //TODO code duplication!!!
        val funcNameOrValue    = iterator.next("function name or value")

        val argumentsTypes     = arrayListOf<TypeToken>()
        val argumentValues     = arrayListOf<AnyValueToken>()
        tryParseArgumentBlock(argumentsTypes, argumentValues)

        val br = iterator.expect<Identifier>("'br' keyword")
        if (br.string != "br") {
            throw ParseErrorException("'br' keyword", br)
        }

        val target = iterator.expect<LabelUsage>("label name")

        val returnType = functionReturnType.type(moduleBuilder)
        when (funcNameOrValue) {
            is SymbolValue -> when (returnType) {
                is TupleType -> {
                    val prototype = builder.makePrototype(funcNameOrValue, returnType, argumentsTypes)
                    builder.tupleCall(currentTok, prototype, argumentValues, target)
                }
                else -> {
                    val prototype = builder.makePrototype(funcNameOrValue, returnType, argumentsTypes)
                    builder.call(currentTok, prototype, argumentValues, target)
                }
            }

            is LocalValueToken -> when (returnType) {
                is TupleType -> {
                    TODO()
                }
                else -> {
                    val prototype = builder.makePrototype(returnType, argumentsTypes)
                    builder.icall(currentTok, funcNameOrValue, prototype, argumentValues, target)
                }
            }
            else -> throw ParseErrorException("function name or value", funcNameOrValue)
        }
    }

    private fun parseVCall() {
        val functionReturnType = iterator.expect<TypeToken>("function type") //TODO code duplication!!!
        val funcNameOrValue    = iterator.next("function name or value")

        val argumentsTypes = arrayListOf<TypeToken>()
        val argumentValues = arrayListOf<AnyValueToken>()
        tryParseArgumentBlock(argumentsTypes, argumentValues)

        val br = iterator.expect<Identifier>("'br' keyword")
        if (br.string != "br") {
            throw ParseErrorException("'br' keyword", br)
        }

        val target = iterator.expect<LabelUsage>("label name")

        val returnType = functionReturnType.type(moduleBuilder)
        when (funcNameOrValue) {
            is SymbolValue -> {
                val prototype = builder.makePrototype(funcNameOrValue, returnType, argumentsTypes)
                builder.vcall(prototype, argumentValues, target)
            }
            is LocalValueToken -> {
                val prototype = builder.makePrototype(returnType, argumentsTypes)
                builder.ivcall(funcNameOrValue, prototype, argumentValues, target)
            }
            else -> throw ParseErrorException("function name or value", funcNameOrValue)
        }
    }

    private fun parseZext(resultName: LocalValueToken) {
        val operandType = iterator.expect<UnsignedIntegerTypeToken>("unsigned value type")
        val operand     = iterator.expect<LocalValueToken>("cast value")
        iterator.expect<To>("'to' keyword")
        val castValueToken = iterator.expect<UnsignedIntegerTypeToken>("${ZeroExtend.NAME} type")

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

    private fun parseFloat2Int(resultName: LocalValueToken) {
        val operandType = iterator.expect<FloatTypeToken>("value type")
        val operand     = iterator.expect<LocalValueToken>("value to cast")
        iterator.expect<To>("'to' keyword")
        val castValueToken = iterator.expect<IntegerTypeToken>("${FloatToInt.NAME} type")

        builder.fp2int(resultName, operand, operandType, castValueToken)
    }

    private fun parseSext(resultName: LocalValueToken) {
        val operandType = iterator.expect<SignedIntegerTypeToken>("signed value type")
        val operand     = iterator.expect<LocalValueToken>("value to cast")
        iterator.expect<To>("'to' keyword")
        val castValueToken = iterator.expect<SignedIntegerTypeToken>("${SignExtend.NAME} type")

        builder.sext(resultName, operand, operandType, castValueToken)
    }

    private fun parseIcmp(resultName: LocalValueToken) {
        val compareTypeToken = iterator.expect<Identifier>("compare type")
        val operandsTypes    = iterator.expect<PrimitiveTypeToken>("signed integer operands type")
        val first            = iterator.expect<AnyValueToken>("first compare operand")
        iterator.expect<Comma>("','")
        val second           = iterator.expect<AnyValueToken>("second compare operand")

        builder.icmp(resultName, first, compareTypeToken, second, operandsTypes)
    }

    private fun parseFcmp(resultTypeToken: LocalValueToken) {
        val compareTypeToken = iterator.expect<Identifier>("compare type")
        val operandsTypes    = iterator.expect<FloatTypeToken>("floating point operands type")
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
                val cmpValue = iterator.expect<AnyValueToken>("value token")

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
        val type = iterator.expect<PrimitiveTypeToken>("primitive type")
        iterator.expect<Comma>("comma")

        val sourceType = iterator.expect<PointerTypeToken>("type ${Type.Ptr}")
        val source     = iterator.expect<ValueToken>("source value")
        iterator.expect<Comma>("comma")
        val indexType  = iterator.expect<IntegerTypeToken>("index type")
        val index      = iterator.expect<AnyValueToken>("index")

        builder.gep(resultName, type, source, sourceType, index, indexType)
    }

    private fun parseGfp(resultName: LocalValueToken) {
        //%$identifier = gfp $type, ${source.type} {source}, ${index.type} ${index}
        val type = iterator.expect<AggregateTypeToken>("aggregate type")
        iterator.expect<Comma>("comma")

        val sourceType = iterator.expect<PointerTypeToken>("type ${Type.Ptr}")
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

        val t1 = iterator.expect<IntegerTypeToken>("primitive type")
        val v1 = iterator.expect<AnyValueToken>("first operand")
        iterator.expect<Comma>("','")

        val t2 = iterator.expect<IntegerTypeToken>("primitive type")
        val v2 = iterator.expect<AnyValueToken>("second operand")

        if (t1.type() != t2.type()) {
            throw ParseErrorException("should be the same primitive type: t1=${t1.type()}, t2=${t2.type()}")
        }

        builder.select(currentTok, v0, v1, v2, t1)
    }

    private fun parseProjection(currentTok: LocalValueToken) {
        // %$identifier = proj {tuple_type}, {type} {tuple_value}, {index}
        val tupleType = iterator.expect<TupleTypeToken>("tuple type")
        iterator.expect<Comma>("','")
        val returnType = iterator.expect<PrimitiveTypeToken>("return type")
        val tupleValue = iterator.expect<LocalValueToken>("tuple value")
        iterator.expect<Comma>("','")
        val index = iterator.expect<IntValue>("index")
        builder.proj(currentTok, tupleType, tupleValue, returnType, index)
    }

    private fun parseFlag2Int(currentTok: LocalValueToken) {
        // %$identifier = flag2int %{value} to {operand type}
        val source = iterator.expect<LocalValueToken>("source value")
        iterator.expect<To>("'to' keyword")
        val type = iterator.expect<IntegerTypeToken>("integer type")
        builder.flag2int(currentTok, source, type)
    }

    private fun parseInt2Float(currentTok: LocalValueToken) {
        // %$identifier = int2fp %{type} %{value} to {operand type}
        val dstType = iterator.expect<IntegerTypeToken>("integer type")
        val source = iterator.expect<LocalValueToken>("source value")
        iterator.expect<To>("'to' keyword")
        val type = iterator.expect<FloatTypeToken>("floating point type")
        builder.int2Float(currentTok, source, dstType, type)
    }

    private fun parsePointer2Int(currentTok: LocalValueToken) {
        // %$identifier = ptr2int %{pointer_type} %{value} to {operand type}
        val pointerType = iterator.expect<PointerTypeToken>("pointer type")
        val source = iterator.expect<LocalValueToken>("source value")

        iterator.expect<To>("'to' keyword")
        val intType = iterator.expect<IntegerTypeToken>("integer type")
        builder.ptr2int(currentTok, source, pointerType, intType)
    }

    private fun parseMemcpy() {
        // memcpy %{dstType} %{dst}, %{srcType} %{src}, %{lengthType} %{length}
        val dstType = iterator.expect<PointerTypeToken>("destination type")
        val dst = iterator.expect<ValueToken>("destination value")
        iterator.expect<Comma>("','")

        val srcType = iterator.expect<PointerTypeToken>("source type")
        val src = iterator.expect<ValueToken>("source value")
        iterator.expect<Comma>("','")

        val lengthType = iterator.expect<UnsignedIntegerTypeToken>("length type")
        val length = iterator.expect<IntValue>("length value")
        val constant = Constant.valueOf<UnsignedIntegerConstant>(lengthType.type(), length.int)
        builder.memcpy(dst, dstType, src, srcType, constant)
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
                    "div"        -> parseDiv(currentTok)
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
                    "flag2int"   -> parseFlag2Int(currentTok)
                    "int2fp"     -> parseInt2Float(currentTok)
                    "bitcast"    -> parseBitcast(currentTok)
                    "fptrunc"    -> parseFptrunc(currentTok)
                    "fpext"      -> parseFpext(currentTok)
                    FloatToInt.NAME  -> parseFloat2Int(currentTok)
                    Int2Pointer.NAME -> parseInt2Pointer(currentTok)
                    Pointer2Int.NAME -> parsePointer2Int(currentTok)
                    Alloc.NAME       -> parseStackAlloc(currentTok)
                    "phi"        -> parsePhi(currentTok)
                    "gep"        -> parseGep(currentTok)
                    "neg"        -> parseNeg(currentTok)
                    "not"        -> parseNot(currentTok)
                    "icmp"       -> parseIcmp(currentTok)
                    "fcmp"       -> parseFcmp(currentTok)
                    "gfp"        -> parseGfp(currentTok)
                    "select"     -> parseSelect(currentTok)
                    Projection.NAME -> parseProjection(currentTok)
                    else -> throw ParseErrorException("instruction name", instruction)
                }
            }
            is LabelDefinition -> builder.switchLabel(currentTok)
            is Identifier -> {
                when (currentTok.string) {
                    Return.NAME -> parseRet()
                    Call.NAME   -> parseVCall()
                    Store.NAME  -> parseStore()
                    Branch.NAME -> parseBranch()
                    Switch.NAME -> parseSwitch()
                    Memcpy.NAME -> parseMemcpy()
                    else        -> throw ParseErrorException("instruction", currentTok)
                }
            }

            else -> throw ParseErrorException("instruction", currentTok)
        }
    }

    private fun parseSwitch() {
        // switch {int_type} {value}, label {default}, [ {value} : {label}, ... ]
        val intType  = iterator.expect<IntegerTypeToken>("integer type")
        val selector = iterator.expect<AnyValueToken>("value")

        iterator.expect<Comma>("','")
        val default = iterator.expect<LabelUsage>("'label' keyword")

        iterator.expect<OpenSquareBracket>("'['")
        val table = arrayListOf<IntValue>()
        val targets = arrayListOf<LocalValueToken>()
        do {
            val value = iterator.expect<IntValue>("value")
            iterator.expect<Colon>("':'")
            val label = iterator.expect<LocalValueToken>("label")
            table.add(value)
            targets.add(label)

            val comma = iterator.next("',' or ']'")
            if (comma is CloseSquareBracket) {
                break
            }

            if (comma !is Comma) {
                throw ParseErrorException("','", comma)
            }
        } while (true)

        builder.switch(selector, default, intType, table, targets)
    }

    private fun parseInt2Pointer(currentTok: LocalValueToken) {
        // %$identifier = int2ptr %{int_type} %{value}
        val intType = iterator.expect<IntegerTypeToken>("integer type")
        val source = iterator.expect<LocalValueToken>("source value")

        iterator.expect<To>("'to' keyword")
        iterator.expect<PointerTypeToken>("pointer type")
        builder.int2ptr(currentTok, source, intType)
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
        fun parse(tokenIterator: TokenIterator, moduleBuilder: ModuleBuilderWithContext, builder: FunctionDataBuilderWithContext) {
            FunctionBlockReader(tokenIterator, moduleBuilder, builder).parseInstructions()
        }
    }
}