import org.junit.jupiter.api.Test
import tokenizer.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class TokenizerTest {
    fun AnyToken.isEqual(l: Int, p: Int, string: String) {
        this as Token
        assertEquals(l, line)
        assertEquals(p, pos)
        assertEquals(string, str())
    }

    @Test
    fun test0() {
        val tokens = Tokenizer.apply("4566,")
        tokens as Token
        assertTrue { tokens is Numeric }
        tokens.isEqual(1, 1, "4566")

        assertTrue { tokens.next is Punct }
        tokens.next.isEqual(1, 5, ",")
        assertTrue { !Token.hasSpace(tokens, tokens.next as Token) }
    }

    @Test
    fun test1() {
        val tokens = Tokenizer.apply("\"sdfsdf\" \"   \"")
        tokens as Token
        assertTrue { tokens is StringLiteral }
        tokens.isEqual(1, 1, "\"sdfsdf\"")

        assertTrue { tokens.next is StringLiteral }
        tokens.next.isEqual(1, 10, "\"   \"")
        assertTrue { Token.hasSpace(tokens, tokens.next as Token) }
    }

    @Test
    fun test2() {
        val tokens = Tokenizer.apply("4.7 /* comment */ 6")
        tokens as Token
        assertTrue { tokens is Numeric }
        tokens.isEqual(1, 1, "4.7")

        assertTrue { tokens.next is Numeric }
        tokens.next.isEqual(1, 19, "6")
        assertTrue { Token.hasSpace(tokens, tokens.next as Token) }
    }

    @Test
    fun test3() {
        val tokens = Tokenizer.apply("+++")
        tokens as Token
        tokens.isEqual(1, 1, "++")
        tokens.next.isEqual(1, 3, "+")
    }
}