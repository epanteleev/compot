package preprocess

import tokenizer.AnyToken
import tokenizer.Ident
import tokenizer.Token


data class Preprocesssor(private val firstToken: AnyToken) {
    private var cond_incl: Boolean = false
    private val macroses = mutableSetOf<Macros>()

//    fun read_macro_definition() {
//        val id = tokens.peek()
//        tokens.removeCurrent()
//        if (id !is Ident) {
//            error("expect")
//        }
//
//
//        val next = tokens.peek()
//
//        if (!Token.hasSpace(id, next) && next.str() == "(") {
//            TODO()
//        } else {
//            macroses.add(Macros(id.str(), tokens.readAndRemoveUntilEol()))
//        }
//    }
//
//    private fun tryExpandMacro(id: Token): Boolean {
//        val macro = macroses.find { it.name == id.str() } ?: return false
//
//        tokens.append(macro.value)
//
//        return true
//    }
//
//    fun preprocess2() {
//        while (!tokens.eof) {
//            var tok = tokens.read()
//
//            if (tryExpandMacro(tok)) {
//                continue
//            }
//
//            if (tok.str() != "#") {
//                continue
//            }
//            tok = tokens.read()
//
//            when (tok.str()) {
//                "define" -> {
//                    read_macro_definition()
//                    continue
//                }
//                else -> {
//
//                }
//            }
//        }
//    }
//
    fun preprocess(): Token {
//        preprocess2()
//        if (cond_incl) {
//            error("unterminated conditional directive")
//        }
//
//        return tokens
        TODO()
    }
}