package preprocess

import tokenizer.*


data class Preprocesssor(private val original: MutableList<CToken>) {
    private var current: Int = 0
    private val macroses = mutableSetOf<Macros>()

    private fun eat(): CToken {
        return original[current++]
    }

    private fun peek(): CToken {
        return original[current]
    }

    private fun removeCurrent() {
        original.removeAt(current)
    }

    private fun eof(): Boolean {
        return current >= original.size
    }

    private fun check(token: String): Boolean {
        return !eof() && peek().str() == token
    }


    fun readAndRemoveUntilEol(tokens: MutableList<CToken>): List<CToken> {
        val subLine = mutableListOf<CToken>()
        val line = tokens[0].line()
        while (tokens.isNotEmpty() && tokens[0].line() == line) {
            subLine.add(tokens.removeFirst())
        }

        return subLine
    }

    fun preprocess(): List<CToken> {
        val result = mutableListOf<CToken>()
        while (!eof()) {
            if (!check("#")) {
                macroses.forEach { macros ->
                    if (peek().str() == macros.name) {
                        removeCurrent()
                        result.addAll(macros.value)
                    }
                }
                continue
            }
            eat()
            val directive = eat()
            if (directive !is Ident) {
                throw IllegalStateException("Expected identifier after #")
            }
            when (directive.str()) {
                "define" -> {
                    val name = eat()
                    if (name !is Ident) {
                        throw IllegalStateException("Expected identifier after #define")
                    }
                    val tokens = mutableListOf<CToken>()
                    while (!eof() && peek().line() == directive.line()) {
                        tokens.add(eat())
                    }
                    val macros = Macros(name.str(), tokens)
                    macroses.add(macros)
                }

                "undef" -> {
                    val name = eat()
                    if (name !is Ident) {
                        throw IllegalStateException("Expected identifier after #undef")
                    }
                    macroses.removeIf { it.name == name.str() }
                }

                "ifdef" -> {
                    val name = eat()
                    if (name !is Ident) {
                        throw IllegalStateException("Expected identifier after #ifdef")
                    }
                    val isDefined = macroses.any { it.name == name.str() }
                    val tokens = mutableListOf<CToken>()
                    while (!eof() && peek().line() == directive.line()) {
                        tokens.add(eat())
                    }
                    if (isDefined) {
                        result.addAll(tokens)
                    }
                }

                "ifndef" -> {
                    val name = eat()
                    if (name !is Ident) {
                        throw IllegalStateException("Expected identifier after #ifndef")
                    }
                    val isDefined = macroses.any { it.name == name.str() }
                    val tokens = mutableListOf<CToken>()
                    while (!eof() && peek().line() == directive.line()) {
                        tokens.add(eat())
                    }
                    if (!isDefined) {
                        result.addAll(tokens)
                    }
                }

                "else" -> {
                    val tokens = mutableListOf<CToken>()
                    while (!eof() && peek().line() == directive.line()) {
                        tokens.add(eat())
                    }
                    result.addAll(tokens)
                }

                "endif" -> {
                    val tokens = mutableListOf<CToken>()
                    while (!eof() && peek().line() == directive.line()) {
                        tokens.add(eat())
                    }
                }

                "include" -> {
                    val name = eat()
                    if (name !is Ident) {
                        throw IllegalStateException("Expected identifier after #include")
                    }
                    val tokens = mutableListOf<CToken>()
                    while (!eof() && peek().line() == directive.line()) {
                        tokens.add(eat())
                    }
                }

                else -> {
                    throw IllegalStateException("Unknown directive ${directive.str()}")
                }
            }
        }
        return result
    }
}