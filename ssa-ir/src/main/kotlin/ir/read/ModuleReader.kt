package ir.read

import ir.instruction.ArithmeticBinaryOp
import ir.instruction.CastType
import ir.module.Module
import ir.read.bulder.FunctionDataBuilderWithContext
import ir.read.bulder.ModuleBuilderWithContext
import ir.read.bulder.ParseErrorException
import ir.types.Type

private class FunctionBlockReader private constructor(private val iterator: TokenIterator, private val builder: FunctionDataBuilderWithContext) {
    private fun parseOperand(expectMessage: String): ValueToken {
        return iterator.expect<ValueToken>(expectMessage)
    }

    private fun parseBinary(resultName: ValueInstructionToken, op: ArithmeticBinaryOp) {
        val resultType = iterator.expect<PrimitiveTypeToken>("result type")
        val first      = parseOperand("first operand")
        iterator.expect<Comma>("','")

        val second = parseOperand("second operand")
        builder.arithmeticBinary(resultName, first, op, second, resultType)
    }

    private fun parseLoad(resultName: ValueInstructionToken) {
        val typeToken    = iterator.expect<PrimitiveTypeToken>("loaded type")
        val pointerToken = iterator.expect<ValueInstructionToken>("pointer")

        builder.load(resultName, pointerToken, typeToken)
    }

    private fun parseStackAlloc(resultName: ValueInstructionToken) {
        val typeToken = iterator.expect<TypeToken>("loaded type")
        builder.stackAlloc(resultName, typeToken)
    }

    private fun parseStore() {
        val type         = iterator.expect<PrimitiveTypeToken>("stored value type")
        val pointerToken = iterator.expect<ValueInstructionToken>("pointer")

        iterator.expect<Comma>("','")
        val valueToken = parseOperand("stored value")
        builder.store(pointerToken, valueToken, type)
    }

    private fun parseRet() {
        val retType     = iterator.expect<PrimitiveTypeToken>("return type")
        if (retType.type() == Type.Void) {
            builder.retVoid()
            return
        }

        val returnValue = parseOperand("value or literal")
        builder.ret(returnValue, retType)
    }

    private fun parseCall(currentTok: ValueInstructionToken?) {
        val functionReturnType = iterator.expect<PrimitiveTypeToken>("function type")
        val functionName       = iterator.expect<FunctionName>("function name")
        val argumentsType      = arrayListOf<PrimitiveTypeToken>()
        val argumentValue      = arrayListOf<ValueToken>()

        iterator.expect<OpenParen>("'('")
        var valueToken = iterator.next("value")
        while (valueToken !is CloseParen) {
            if (valueToken !is ValueToken) {
                throw ParseErrorException("value", valueToken)
            }

            iterator.expect<Colon>("':'")
            val type = iterator.expect<PrimitiveTypeToken>("argument type")

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

    private fun parseCast(resultName: ValueInstructionToken, castType: CastType) {
        val operandType = iterator.expect<PrimitiveTypeToken>("value type")
        val operand   = iterator.expect<ValueInstructionToken>("cast value")
        iterator.expect<To>("'to' keyword")
        val castValueToken = iterator.expect<PrimitiveTypeToken>("cast type")

        builder.cast(resultName, operand, operandType, castType, castValueToken)
    }

    private fun parseCmp(resultTypeToken: ValueInstructionToken) {
        val compareTypeToken = iterator.expect<Identifier>("compare type")
        val resultType       = iterator.expect<PrimitiveTypeToken>("result type")
        val first            = iterator.expect<ValueToken>("compare operand")
        iterator.expect<Comma>("','")
        val second           = iterator.expect<ValueToken>("compare operand")

        builder.intCompare(resultTypeToken, first, compareTypeToken, second, resultType)
    }

    private fun parsePhi(resultTypeToken: ValueInstructionToken) {
        val type = iterator.expect<PrimitiveTypeToken>("operands type")

        iterator.expect<OpenSquareBracket>("'['")
        val labels = arrayListOf<Identifier>()
        val argumentValue = arrayListOf<ValueToken>()

        do {
            val value = iterator.expect<ValueToken>("value")
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
                val cmpValue = iterator.expect<ValueInstructionToken>("value type")

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

    private fun parseGep(resultName: ValueInstructionToken) {
        //%$identifier = gep $tp {source}, ${index.type} ${index}
        val sourceType = iterator.expect<PrimitiveTypeToken>("type")
        val source     = iterator.expect<ValueInstructionToken>("source value")
        iterator.expect<Comma>("comma")
        val indexType  = iterator.expect<PrimitiveTypeToken>("index type")
        val index      = iterator.expect<ValueToken>("index")

        builder.gep(resultName, source, sourceType, index, indexType)
    }

    private fun parseInstruction(currentTok: Token) {
        when (currentTok) {
            is ValueInstructionToken -> {
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
                    "sext"       -> parseCast(currentTok, CastType.SignExtend)
                    "zext"       -> parseCast(currentTok, CastType.ZeroExtend)
                    "trunc"      -> parseCast(currentTok, CastType.Truncate)
                    "bitcast"    -> parseCast(currentTok, CastType.Bitcast)
                    "alloc"      -> parseStackAlloc(currentTok)
                    "icmp"       -> parseCmp(currentTok)
                    "phi"        -> parsePhi(currentTok)
                    "gep"        -> parseGep(currentTok)
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

    private fun parseExtern() {
        //extern <returnType> <function name> ( <type1>, <type2>, ...)
        val returnType = tokenIterator.expect<PrimitiveTypeToken>("return type")
        val functionName = tokenIterator.expect<FunctionName>("function name")

        tokenIterator.expect<OpenParen>("'('")

        val argumentsType = arrayListOf<PrimitiveTypeToken>()
        do {
            val type = tokenIterator.expect<PrimitiveTypeToken>("argument type")
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
        val returnType = tokenIterator.expect<PrimitiveTypeToken>("return type")
        val functionName = tokenIterator.expect<FunctionName>("function name")

        tokenIterator.expect<OpenParen>("'('")
        val argumentsType = arrayListOf<PrimitiveTypeToken>()
        val argumentValue = arrayListOf<ValueInstructionToken>()

        do {
            val value = tokenIterator.next("value")
            if (value is CloseParen) {
                break
            }
            if (value !is ValueInstructionToken) {
                throw ParseErrorException("value ", value)
            }

            tokenIterator.expect<Colon>("':'")
            val type = tokenIterator.expect<PrimitiveTypeToken>("argument type")

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