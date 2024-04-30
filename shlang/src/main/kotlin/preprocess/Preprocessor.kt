package preprocess

import common.AnyParser
import tokenizer.*


class Preprocesssor(original: MutableList<CToken>): AnyParser(original) {
    private val macroses = mutableSetOf<Macros>()

    private fun isLineEnd(): Boolean {
        return eof() || peak<CToken>().line() != peak<CToken>(-1).line()
    }

    fun preprocess(): List<CToken> {
        val result = mutableListOf<CToken>()
        while (!eof()) {
            if (!check("#")) {
                macroses.forEach { macros ->
                    val tok = peak<CToken>()
                    if (tok.str() == macros.name) {
                        result.addAll(macros.cloneContentWith(tok.position()))
                        eat()
                        return@forEach
                    }
                    result.add(peak())
                    eat()
                }

                continue
            }
            eat()
            val directive = peak<Ident>()
            eat()
            when (directive.str()) {
                "define" -> {
                    val name = peak<Ident>()
                    eat()
                    val value = mutableListOf<CToken>()
                    while (!isLineEnd()) {
                        value.add(peak())
                        eat()
                    }
                    macroses.add(Macros(name.str(), value))
                }
                else -> {
                    throw IllegalStateException("Unknown directive ${directive.str()}")
                }
            }
        }
        return result
    }
}