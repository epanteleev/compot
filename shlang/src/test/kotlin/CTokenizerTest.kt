import org.junit.jupiter.api.Test
import tokenizer.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class CTokenizerTest {
    fun AnyToken.isEqual(l: Int, p: Int, string: String) {
        this as CToken
        assertEquals(l, line)
        assertEquals(p, pos)
        assertEquals(string, str())
    }

    @Test
    fun test0() {
        val tokens = CTokenizer.apply("4566,")
        tokens as CToken
        assertTrue { tokens is Numeric }
        tokens.isEqual(1, 1, "4566")

        assertTrue { tokens.next is Punct }
        tokens.next.isEqual(1, 5, ",")
        assertTrue { !CToken.hasSpace(tokens, tokens.next as CToken) }
    }

    @Test
    fun test1() {
        val tokens = CTokenizer.apply("\"sdfsdf\" \"   \"")
        tokens as CToken
        assertTrue { tokens is StringLiteral }
        tokens.isEqual(1, 1, "\"sdfsdf\"")

        assertTrue { tokens.next is StringLiteral }
        tokens.next.isEqual(1, 10, "\"   \"")
        assertTrue { CToken.hasSpace(tokens, tokens.next as CToken) }
    }

    @Test
    fun test2() {
        val tokens = CTokenizer.apply("4.7 /* comment */ 6")
        tokens as CToken
        assertTrue { tokens is Numeric }
        tokens.isEqual(1, 1, "4.7")

        assertTrue { tokens.next is Numeric }
        tokens.next.isEqual(1, 19, "6")
        assertTrue { CToken.hasSpace(tokens, tokens.next as CToken) }
    }

    @Test
    fun test3() {
        val tokens = CTokenizer.apply("+++")
        tokens as CToken
        tokens.isEqual(1, 1, "++")
        tokens.next.isEqual(1, 3, "+")
    }
}