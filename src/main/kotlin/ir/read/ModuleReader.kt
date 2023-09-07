package ir.read

import ir.*
import ir.builder.FunctionDataBuilder
import ir.builder.ModuleBuilder

class ParseErrorException(expect: String, actual: String): Exception("Found: $actual, but expect $expect")

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

    private fun parseType(): Type {
        val typeToken = tokenIterator.expectOrError<TypeToken>("return type")
        val returnTypeKind = matchType[typeToken.type]
            ?: throw ParseErrorException("unknown type ", typeToken.message())

        return Type.of(returnTypeKind, typeToken.indirection)
    }

    private fun parseFunctionName(): String {
        return tokenIterator.nextOrError("function name").let {
            if (it !is Identifier) {
                throw ParseErrorException("function name", it.message())
            }
            it.string
        }
    }

    private fun parseOperand(builder: FunctionDataBuilder, errorMessage: String): Value {
        return tokenIterator.nextOrError(errorMessage).let {
            when (it) {
                is IntValue   -> I64Value(it.int)
                is FloatValue -> TODO("Unimplemented yet")
                is ValueToken -> builder.findValue(it.name.toInt()) ?:
                    throw RuntimeException("in ${it.line}:${it.pos} undefined value")
                else -> throw ParseErrorException("constant or value", it.message())
            }
        }
    }

    private fun parseExtern() {
        //extern <returnType> <function name> ( <type1>, <type2>, ...)
        val returnType = parseType()
        val functionName = parseFunctionName()

        tokenIterator.expectOrError<OpenParen>("'('")

        val argumentsType = arrayListOf<Type>()
        do {
            val type = parseType()
            argumentsType.add(type)

            val comma = tokenIterator.nextOrError("','")
            if (comma is CloseParen) {
                break
            }

            if (comma !is Comma) {
                throw ParseErrorException("type ", comma.message())
            }
        } while (true)

        moduleBuilder.createExternFunction(functionName, returnType, argumentsType)
    }

    private fun parseFunction() {
        // define <returnType> <functionName>(<value1>:<type1>, <value2>:<type2>,...)
        val returnType = parseType()
        val functionName = parseFunctionName()

        tokenIterator.expectOrError<OpenParen>("'('")
        val argumentsType = arrayListOf<Type>()
        val argumentValue = arrayListOf<ArgumentValue>()

        do {
            val value = tokenIterator.expectOrError<ValueToken>("value")

            tokenIterator.expectOrError<Colon>("';'")
            val type = parseType()
            argumentValue.add(ArgumentValue(value.name.toInt(), type))
            argumentsType.add(type)

            val comma = tokenIterator.nextOrError("','")
            if (comma is CloseParen) {
                break
            }

            if (comma !is Comma) {
                throw ParseErrorException("type ", comma.message())
            }
        } while (true)

        val fn = moduleBuilder.createFunction(functionName, returnType, argumentsType, argumentValue)

        tokenIterator.expectOrError<OpenBrace>("'{'")

        val tok = tokenIterator.next()
        while (tok !is OpenBrace) {
            val dot = tokenIterator.nextOrError("'.'")
            if (dot is Dot) {
                val label = tokenIterator.expectOrError<Identifier>("label name")
            }
            
            tokenIterator.expectOrError<Colon>("label name with ':'")

            parseInstruction(fn, tok)
        }
    }

    private fun parseInstruction(builder: FunctionDataBuilder, currentTok: Token) {
        if (currentTok is ValueToken) {
            tokenIterator.expectOrError<Equal>("'='")

            val instruction = tokenIterator.expectOrError<Identifier>("instruction name")
            if (instruction.string == "add") {
                val resultType = tokenIterator.expectOrError<TypeToken>("result type")
                val first = parseOperand(builder, "first operand")
                tokenIterator.expectOrError<Comma>("','")

                val second = parseOperand(builder, "second operand")

                builder.arithmeticBinary(first, ArithmeticBinaryOp.Add, second)
            }
        }
    }

    fun read(): Module {
        parseModule()
        return moduleBuilder.build()
    }

    companion object {
        private val matchType = hashMapOf(
            "u1"   to TypeKind.U1,
            "u8"   to TypeKind.U8,
            "u16"  to TypeKind.U16,
            "u32"  to TypeKind.U32,
            "u64"  to TypeKind.U64,
            "i8"   to TypeKind.I8,
            "i16"  to TypeKind.I16,
            "i32"  to TypeKind.I32,
            "i64"  to TypeKind.I16,
            "void" to TypeKind.VOID
        )
    }
}