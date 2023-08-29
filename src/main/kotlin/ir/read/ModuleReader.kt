package ir.read

import ir.Module
import ir.Type
import ir.TypeKind
import ir.builder.ModuleBuilder

class ParseErrorException(expect: String, actual: String): Exception("Expect: $expect, but found $actual")

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
        do {
            val value = tokenIterator.expectOrError<ValueToken>("value")

            tokenIterator.expectOrError<Colon>("';'")
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

        val fn = moduleBuilder.createFunction(functionName, returnType, argumentsType)
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