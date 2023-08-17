package ir.read

import ir.Module
import ir.Type
import ir.builder.ModuleBuilder

class ParseErrorException(expect: String, actual: String): Exception("Expect: $expect, but found $actual")

class ModuleReader(string: String) {
    private val tokenIterator = Tokenizer(string).iterator()
    private val moduleBuilder = ModuleBuilder()

    fun isTok(token: Token): Boolean {
        return false
    }

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
    }

    private fun parseExtern() {
        val functionName = tokenIterator.nextOrError("function name")
        if (functionName !is Identifier) {
            throw ParseErrorException("function name", functionName.message())
        }

        val openParen = tokenIterator.nextOrError("'('")
        if (openParen !is OpenParen) {
            throw ParseErrorException("'('", functionName.message())
        }

        var tok = tokenIterator.nextOrError("')' or type")
        while (tok !is CloseParen) {
            if (tok !is TypeToken) {
                throw ParseErrorException("type ", tok.message())
            }

        }
    }

    fun read(): Module {
        parseModule()
        return moduleBuilder.build()
    }

    companion object {
        private val matchType = hashMapOf(
            "u1"  to Type.U1,
            "u8"  to Type.U8,
            "u16" to Type.U16,
            "u32" to Type.U32,
            "u64" to Type.U64,
            "i8"  to Type.I8,
            "i16" to Type.I16,
            "i32" to Type.I32,
            "i64" to Type.I16
        )
    }
}