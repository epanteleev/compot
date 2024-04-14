package tokenizer

object LexicalElements {
    // Annex A: (6.4.1)
    val keywords = setOf(
        "auto", "break", "case", "char", "const", "continue", "default", "do", "double", "else", "enum", "extern",
        "float", "for", "goto", "if", "inline", "int", "long", "register", "restrict", "return", "short", "signed",
        "sizeof", "static", "struct", "switch", "typedef", "union", "unsigned", "void", "volatile", "while",
        "_Alignas", "_Alignof", "_Atomic", "_Bool", "_Complex", "_Generic", "_Imaginary", "_Noreturn", "_Static_assert", "_Thread_local"
    )

    // (6.7.1) storage-class-specifier:
    val storageClassSpecifiers = setOf("typedef", "extern", "static", "_Thread_local", "auto", "register")

    // (6.7.2) type-specifier:
    val typeSpecifier = setOf("void", "char", "short", "int", "long", "float", "double", "signed", "unsigned", "_Bool", "_Complex")

    // (6.5.3) Unary operators
    val unaryOperators = setOf("&", "*", "+", "-", "~", "!")

    val assignmentOperators = setOf("=", "*=", "/=", "%=", "+=", "-=", "<<=", ">>=", "&=", "^=", "|=")
    val binaryOperators = setOf(
        "*", "/", "%",
        "+", "-",
        "<<", ">>",
        "<", ">", "<=", ">=",
        "==", "!=",
        "&", "^", "|",
        "&&", "||"
    )
    val ternaryOperators = setOf("?", ":")
    val postPreFixOperators = setOf("++", "--")

    val allOperators = unaryOperators + binaryOperators + ternaryOperators + postPreFixOperators + assignmentOperators
    val allSymbols = allOperators + setOf("->", "(", ")", "[", "]", "{", "}", ";", ",", ".", "...", "#", "##", "\\")
    val operators2 = allSymbols.filter { it.length == 2 }
    val operators3 = allSymbols.filter { it.length == 3 }

    fun isOperator2(ch1: Char, ch2: Char): Boolean {
        for (operator in operators2) {
            if (operator[0] == ch1 && operator[1] == ch2) {
                return true
            }
        }
        return false
    }

    fun isOperator3(ch1: Char, ch2: Char, ch3: Char): Boolean {
        for (operator in operators3) {
            if (operator[0] == ch1 && operator[1] == ch2 && operator[2] == ch3) {
                return true
            }
        }
        return false
    }
}