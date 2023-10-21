package ir.read

import ir.instruction.ArithmeticBinaryOp
import ir.instruction.CastType
import ir.module.Module
import ir.read.bulder.FunctionDataBuilderWithContext
import ir.read.bulder.ModuleBuilderWithContext
import ir.read.bulder.ParseErrorException

private class FunctionBlockReader private constructor(private val iterator: TokenIterator, private val builder: FunctionDataBuilderWithContext) {
    private fun parseOperand(errorMessage: String): ValueToken {
        return iterator.expect<ValueToken>(errorMessage)
    }

    private fun parseBinary(resultName: ValueInstructionToken, op: ArithmeticBinaryOp) {
        val resultType = iterator.expect<TypeToken>("result type")
        val first      = parseOperand("first operand")
        iterator.expect<Comma>("','")

        val second = parseOperand("second operand")
        builder.arithmeticBinary(resultName, first, op, second, resultType)
    }

    private fun parseLoad(resultName: ValueInstructionToken) {
        val typeToken    = iterator.expect<TypeToken>("loaded type")
        val pointerToken = iterator.expect<ValueInstructionToken>("pointer")

        builder.load(resultName, pointerToken, typeToken)
    }

    private fun parseStackAlloc(resultName: ValueInstructionToken) {
        val typeToken      = iterator.expect<TypeToken>("loaded type")
        val allocationSize = iterator.expect<IntValue>("stackalloc size")

        builder.stackAlloc(resultName, typeToken, allocationSize)
    }

    private fun parseStore() {
        val type         = iterator.expect<TypeToken>("stored value type")
        val pointerToken = iterator.expect<ValueInstructionToken>("pointer")

        iterator.expect<Comma>("','")
        val valueToken = parseOperand("stored value")
        builder.store(pointerToken, valueToken, type)
    }

    private fun parseRet() {
        val retType     = iterator.expect<TypeToken>("return type")
        val returnValue = parseOperand("value or literal")

        builder.ret(returnValue, retType)
    }

    private fun parseCall(currentTok: ValueInstructionToken?) {
        val functionReturnType = iterator.expect<TypeToken>("function type")
        val functionName       = iterator.expect<Identifier>("function name")
        val argumentsType      = arrayListOf<TypeToken>()
        val argumentValue      = arrayListOf<ValueToken>()

        iterator.expect<OpenParen>("'('")
        var valueToken = iterator.next("value")
        while (valueToken !is CloseParen) {
            if (valueToken !is ValueToken) {
                throw ParseErrorException("value", valueToken)
            }

            iterator.expect<Colon>("':'")
            val type = iterator.expect<TypeToken>("argument type")

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
        val castToken      = iterator.expect<TypeToken>("cast type")
        val castValueToken = iterator.expect<ValueInstructionToken>("cast value")

        builder.cast(resultName, castValueToken, castToken, castType, castToken) //Todo
    }

    private fun parseCmp(resultTypeToken: ValueInstructionToken) {
        val compareTypeToken = iterator.expect<Identifier>("compare type")
        val resultType       = iterator.expect<TypeToken>("result type")
        val first            = iterator.expect<ValueToken>("compare operand")
        iterator.expect<Comma>("','")
        val second           = iterator.expect<ValueToken>("compare operand")

        builder.intCompare(resultTypeToken, first, compareTypeToken, second, resultType)
    }

    private fun parsePhi(resultTypeToken: ValueInstructionToken) {
        val type = iterator.expect<TypeToken>("operands type")

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
        val labelOrType = iterator.next("'label' or type")
        if (labelOrType is Identifier) {
            // br label {labelName}
            if (labelOrType.string == "label") {
                val labelName = iterator.expect<Identifier>("label name")
                builder.branch(labelName)
            } else {
                throw ParseErrorException("'label'", labelOrType)
            }
        } else if (labelOrType is TypeToken) {
            // br {cmpValue} label {trueLabel}, label {falseLabel}
            val cmpValue = iterator.expect<ValueInstructionToken>("value type")
            if (iterator.expect<Identifier>("'label'").string != "label") {
                throw ParseErrorException("label name", labelOrType)
            }

            val trueLabel = iterator.expect<Identifier>("label name")
            iterator.expect<Comma>("','")
            if (iterator.expect<Identifier>("'label'").string != "label") {
                throw ParseErrorException("label name", labelOrType)
            }

            val labelFalse = iterator.expect<Identifier>("label name")
            builder.branchCond(cmpValue, trueLabel, labelFalse)
        } else {
            throw ParseErrorException("'label' or type", labelOrType)
        }
    }

    private fun parseGep(resultName: ValueInstructionToken) {
        //%$identifier = gep $tp {source}, ${index.type} ${index}
        val sourceType = iterator.expect<TypeToken>("type")
        val source     = iterator.expect<ValueInstructionToken>("source value")
        iterator.expect<Comma>("comma")
        val indexType  = iterator.expect<TypeToken>("index type")
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
                    "alloc" -> parseStackAlloc(currentTok)
                    "icmp"       -> parseCmp(currentTok)
                    "phi"        -> parsePhi(currentTok)
                    "gep"        -> parseGep(currentTok)
                    else -> throw ParseErrorException("instruction name", instruction)
                }
            }

            is LabelToken -> {
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

    private fun parseFunctionName(): Identifier {
        return tokenIterator.next("function name").let {
            if (it !is Identifier) {
                throw ParseErrorException("function name", it)
            }

            it
        }
    }

    private fun parseExtern() {
        //extern <returnType> <function name> ( <type1>, <type2>, ...)
        val returnType = tokenIterator.expect<TypeToken>("return type")
        val functionName = parseFunctionName()

        tokenIterator.expect<OpenParen>("'('")

        val argumentsType = arrayListOf<TypeToken>()
        do {
            val type = tokenIterator.expect<TypeToken>("argument type")
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
        val returnType = tokenIterator.expect<TypeToken>("return type")
        val functionName = parseFunctionName()

        tokenIterator.expect<OpenParen>("'('")
        val argumentsType = arrayListOf<TypeToken>()
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
            val type = tokenIterator.expect<TypeToken>("argument type")

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