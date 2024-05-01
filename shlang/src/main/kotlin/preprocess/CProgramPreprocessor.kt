package preprocess

import common.AnyParser
import tokenizer.*

data class PreprocessorException(override val message: String): Exception(message)


class CProgramPreprocessor(original: TokenIterator): AnyParser(original.tokens()) {
    private val macroses = mutableSetOf<Macros>()

    fun preprocess(): List<AnyToken> {
        val result = mutableListOf<AnyToken>()
        while (!eof()) {
            if (check<NewLine>()) {
                result.add(peak())
                eat()
                continue
            }
            if (check<Indent>()) {
                result.add(peak())
                eat()
                continue
            }
            if (!check("#")) {
                val tok = peak<CToken>()
                val macros = macroses.find { it.name == tok.str()}
                if (macros == null) {
                    result.add(peak())
                    eat()
                    continue
                }
                eat()
                result.addAll(macros.cloneContentWith(tok.position()))
                continue
            }
            eat()
            if (!check<Ident>()) {
                throw PreprocessorException("Expected directive")
            }

            val directive = peak<Ident>()
            eat()
            when (directive.str()) {
                "define" -> {
                    eatWithSpaces()
                    val name = peak<Ident>()
                    eatWithSpaces()
                    val value = mutableListOf<AnyToken>()
                    while (!check<NewLine>()) {
                        value.add(peak())
                        eat()
                    }
                    macroses.add(Macros(name.str(), value))
                }
                else -> throw PreprocessorException("Unknown directive ${directive.str()}")
            }
        }
        return result
    }
}