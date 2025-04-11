package ir.read

import ir.types.*
import ir.global.*
import ir.module.Module
import ir.read.bulder.*
import ir.read.tokens.*
import ir.value.constant.*
import java.io.FileInputStream


class ModuleReader private constructor(string: String) {
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

    private fun makeConstant0(type: TypeToken): NonTrivialConstant = when (val data = tokenIterator.next()) {
        is IntValue -> {
            if (type !is IntegerTypeToken) {
                throw throw ParseErrorException("expect integer type, but: type=${type}")
            }
            IntegerConstant.of(type.type(), data.int)
        }
        is FloatValue -> {
            if (type !is FloatTypeToken) {
                throw throw ParseErrorException("expect float type, but: type=${type}")
            }
            FloatingPointConstant.of(type.type(), data.fp)
        }
        is StringLiteralToken -> {
            if (type !is ArrayTypeToken) {
                throw throw ParseErrorException("expect float type, but: type=${type}")
            }

            StringLiteralConstant(ArrayType(I8Type, data.string.length), data.string)
        }
        is OpenBrace -> {
            if (type !is AggregateTypeToken) {
                throw ParseErrorException("expect aggregate type, but: type=${type}")
            }
            parseInitializerListValue(type)
        }

        else -> throw ParseErrorException("unsupported: data=$data")
    }

    private fun makeConstant(type: TypeToken, name: String): GlobalConstant {
        val global = when (val data = tokenIterator.next()) {
            is IntValue -> {
                if (type !is IntegerTypeToken) {
                    throw throw ParseErrorException("expect integer type, but: type=${type}")
                }

                when (type.type()) {
                    I8Type   -> I8ConstantValue(name, data.int.toByte())
                    U8Type   -> U8ConstantValue(name, data.int.toUByte())
                    I16Type -> I16ConstantValue(name, data.int.toShort())
                    U16Type  -> U16ConstantValue(name, data.int.toUShort())
                    I32Type -> I32ConstantValue(name, data.int.toInt())
                    U32Type  -> U32ConstantValue(name, data.int.toUInt())
                    I64Type -> I64ConstantValue(name, data.int)
                    U64Type -> U64ConstantValue(name, data.int.toULong())
                }
            }
            is FloatValue -> {
                if (type !is FloatTypeToken) {
                    throw throw ParseErrorException("expect float type, but: type=${type}")
                }
                when (type.type()) {
                    F32Type -> F32ConstantValue(name, data.fp.toFloat())
                    F64Type -> F64ConstantValue(name, data.fp)
                }
            }
            is StringLiteralToken -> {
                if (type !is ArrayTypeToken) {
                    throw throw ParseErrorException("expect float type, but: type=${type}")
                }

                StringLiteralGlobalConstant(name, type.type(moduleBuilder), data.string)
            }
            is OpenBrace -> {
                if (type !is AggregateTypeToken) {
                    throw ParseErrorException("expect aggregate type, but: type=${type}")
                }
                parseAggregateInitializer(name, type)
            }

            else -> throw ParseErrorException("unsupported: data=$data")
        }
        return moduleBuilder.addConstant(global)
    }

    // TODO copy paste from makeConstant
    private fun parseInitializerListValue(type: AggregateTypeToken): NonTrivialConstant {
        val fields = arrayListOf<NonTrivialConstant>()
        do {
            val field = tokenIterator.next("field")
            if (field is CloseBrace) {
                break
            }

            if (field !is LiteralValueToken) {
                throw ParseErrorException("constant field", field)
            }

            val fieldInitializer = tokenIterator.next("field initializer")
            if (fieldInitializer !is Colon) {
                throw ParseErrorException("':'", fieldInitializer)
            }
            val constType = tokenIterator.expect<TypeToken>("field type") //TODO check correct type with declared type

            fields.add(field.toConstant(constType.asType<PrimitiveType>(moduleBuilder)))

            val next = tokenIterator.next("comma or '}'")
            if (next is CloseBrace) {
                break
            }

            if (next !is Comma) {
                throw ParseErrorException("','", next)
            }
        } while (true)

        return InitializerListValue(type.type(moduleBuilder), fields)
    }

    private fun parseAggregateInitializer(name: String, type: AggregateTypeToken): GlobalConstant {
        val fields = arrayListOf<NonTrivialConstant>()
        do {
            val field = tokenIterator.next("field")
            if (field is CloseBrace) {
                break
            }

            if (field !is LiteralValueToken) {
                throw ParseErrorException("constant field", field)
            }

            val fieldInitializer = tokenIterator.next("field initializer")
            if (fieldInitializer !is Colon) {
                throw ParseErrorException("':'", fieldInitializer)
            }
            val constType = tokenIterator.expect<TypeToken>("field type") //TODO check correct type with declared type

            val primitiveType = constType.asType<PrimitiveType>(moduleBuilder)
            fields.add(field.toConstant(primitiveType))

            val next = tokenIterator.next("comma or '}'")
            if (next is CloseBrace) {
                break
            }

            if (next !is Comma) {
                throw ParseErrorException("','", next)
            }
        } while (true)

        return when (type) {
            is StructDefinition -> StructGlobalConstant(name, type.type(moduleBuilder), fields)
            is ArrayTypeToken -> ArrayGlobalConstant(name, type.type(moduleBuilder), fields)
        }
    }

    private fun parseGlobals(name: SymbolValue) {
        tokenIterator.expect<Equal>("'='")
        val keyword = tokenIterator.next("'constant' or 'global'")
        val typeToken = tokenIterator.expect<TypeToken>("constant type")

        when (keyword) {
            is ConstantKeyword -> makeConstant(typeToken, name.name)
            is GlobalKeyword -> {
                val constant = makeConstant0(typeToken) //TODO refactor type resolution
                val type = typeToken.asType<NonTrivialType>(moduleBuilder)
                if (constant.type() != type) {
                    throw ParseErrorException("type mismatch: expected $type, but got ${constant.type()}")
                }
                moduleBuilder.addGlobalValue(name.name, constant)
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

    private fun read(): Module {
        parseModule()
        return moduleBuilder.build()
    }

    companion object {
        fun read(name: String): Module {
            val text = FileInputStream(name).use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            }

            try {
                return ModuleReader(text).read()
            } catch (e: Exception) {
                println("Error: ${e.message}")
                throw e
            }
        }
    }
}