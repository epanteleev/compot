package ir.read

import ir.global.*
import ir.types.*
import ir.module.Module
import ir.read.bulder.*
import ir.read.tokens.*


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
                is StructDefinition -> parseStructDefinition(tok)
                else -> throw ParseErrorException("function, extern or global constant", tok)
            }
        } while (tokenIterator.hasNext())
    }

    private fun parseStructDefinition(struct: StructDefinition) {
        tokenIterator.expect<Equal>("'='")
        tokenIterator.expect<TypeKeyword>("'type' keyword")

        tokenIterator.expect<OpenBrace>("'{'")
        val fieldTypes = arrayListOf<TypeToken>()

        do {
            val tp = tokenIterator.expect<TypeToken>("type")
            fieldTypes.add(tp)

            val next = tokenIterator.next("comma, '}' or type")
            if (next is CloseBrace) {
                break
            }

            if (next !is Comma) {
                throw ParseErrorException("','", next)
            }

        } while (true)

        val resolvedFieldTypes = arrayListOf<NonTrivialType>()
        for (f in fieldTypes) {
            val resolved = f.type(moduleBuilder)
            if (resolved !is NonTrivialType) {
                throw ParseErrorException("")
            }

            resolvedFieldTypes.add(resolved)
        }

        moduleBuilder.structType(struct.name, resolvedFieldTypes)
    }

    private fun makeConstant(type: TypeToken, name: String): GlobalConstant {
        val global = when (val data = tokenIterator.next()) {
            is IntValue -> {
                if (type !is IntegerTypeToken) {
                    throw throw ParseErrorException("expect integer type, but: type=${type}")
                }
                when (val tp = type.type()) {
                    Type.I8 -> I8ConstantValue(name, data.int.toByte())
                    Type.U8 -> U8ConstantValue(name, data.int.toUByte())
                    Type.I16 -> I16ConstantValue(name, data.int.toShort())
                    Type.U16 -> U16ConstantValue(name, data.int.toUShort())
                    Type.I32 -> I32ConstantValue(name, data.int.toInt())
                    Type.U32 -> U32ConstantValue(name, data.int.toUInt())
                    Type.I64 -> I64ConstantValue(name, data.int)
                    Type.U64 -> U64ConstantValue(name, data.int.toULong())
                    else -> throw ParseErrorException("unsupported: type=$tp, data=${data.int}")
                }
            }

            is FloatValue -> {
                if (type !is FloatTypeToken) {
                    throw throw ParseErrorException("expect float type, but: type=${type}")
                }
                when (val tp = type.type()) {
                    Type.F32 -> F32ConstantValue(name, data.fp.toFloat())
                    Type.F64 -> F64ConstantValue(name, data.fp)
                    else -> throw ParseErrorException("unsupported: type=$tp, data=${data.fp}")
                }
            }

            is StringLiteralToken -> {
                if (type !is ArrayTypeToken) {
                    throw throw ParseErrorException("expect float type, but: type=${type}")
                }

                StringLiteralConstant(name, type.type(moduleBuilder), data.string)
            }

            else -> throw ParseErrorException("unsupported: data=$data")
        }
        return moduleBuilder.addConstant(global)
    }

    private fun parseGlobals(name: SymbolValue) {
        tokenIterator.expect<Equal>("'='")
        val keyword = tokenIterator.next("'constant' or 'global'")
        val type = tokenIterator.expect<TypeToken>("constant type")

        when (keyword) {
            is ConstantKeyword -> makeConstant(type, name.name)
            is GlobalKeyword -> {
                val constant = makeConstant(type, "CP_${name.name}")
                moduleBuilder.addGlobal(name.name, constant)
            }
            else -> throw ParseErrorException("constant or global", keyword)
        }
    }

    private fun parseExtern() {
        //extern <returnType> <function name> ( <type1>, <type2>, ...)
        val returnType = tokenIterator.expect<TypeToken>("return type")
        val functionName = tokenIterator.expect<SymbolValue>("function name")

        tokenIterator.expect<OpenParen>("'('")

        val argumentsType = arrayListOf<TypeToken>()
        do {
            val type = tokenIterator.expect<TypeToken>("argument type")
            argumentsType.add(type)

            val tok = tokenIterator.next("','")
            if (tok is CloseParen) {
                break
            }

            if (tok !is Comma) {
                throw ParseErrorException("type ", tok)
            }
        } while (true)

        moduleBuilder.createExternFunction(functionName, returnType, argumentsType)
    }

    private fun parseFunction() {
        // define <returnType> <functionName>(<value1>:<type1>, <value2>:<type2>,...)
        val returnType = tokenIterator.expect<TypeToken>("return type")
        val functionName = tokenIterator.expect<SymbolValue>("function name")

        tokenIterator.expect<OpenParen>("'('")
        val argumentsType = arrayListOf<TypeToken>()
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
            val type = tokenIterator.expect<TypeToken>("argument type")

            argumentValue.add(value)
            argumentsType.add(type)

            val comma = tokenIterator.next("','")
            if (comma is CloseParen) {
                break
            }

            if (comma !is Comma) {
                throw ParseErrorException("','", comma)
            }
        } while (true)

        val fn = moduleBuilder.createFunction(functionName, returnType, argumentsType, argumentValue)
        FunctionBlockReader.parse(tokenIterator, moduleBuilder, fn)
    }

    fun read(): Module {
        parseModule()
        return moduleBuilder.build()
    }
}