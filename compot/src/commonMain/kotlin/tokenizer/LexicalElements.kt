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

    // 6.4.6 Punctuators
    private val punctuators = unaryOperators + binaryOperators + ternaryOperators + postPreFixOperators + assignmentOperators
    val allPunctuators = punctuators + setOf("->", "(", ")", "[", "]", "{", "}", ";", ",", ".", "...", "#", "##") //TODO add binary operators
    private val operators2 = allPunctuators.filter { it.length == 2 }
    private val operators3 = allPunctuators.filter { it.length == 3 }

    fun isOperator2(ch1: Char, ch2: Char): Boolean { //TODO remove
        for (operator in operators2) {
            if (operator[0] == ch1 && operator[1] == ch2) {
                return true
            }
        }
        return false
    }

    fun isOperator3(ch1: Char, ch2: Char, ch3: Char): Boolean { //TODO remove
        for (operator in operators3) {
            if (operator[0] == ch1 && operator[1] == ch2 && operator[2] == ch3) {
                return true
            }
        }
        return false
    }
}