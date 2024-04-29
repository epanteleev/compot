import org.junit.jupiter.api.Test
import tokenizer.*
import kotlin.test.Ignore
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class CTokenizerTest {
    fun CToken.isEqual(l: Int, p: Int, string: String) {
        assertEquals(l, line())
        assertEquals(p, pos())
        assertEquals(string, str())
    }

    @Test
    fun test0() {
        val tokens = CTokenizer.apply("4566,")

        assertTrue { tokens[0] is Numeric }
        tokens[0].isEqual(1, 1, "4566")
        tokens[1].isEqual(1, 5, ",")
    }

    @Test
    fun test1() {
        val tokens = CTokenizer.apply("\"sdfsdf\" \"   \"")

        assertTrue { tokens[0] is StringLiteral }
        assertEquals("\"sdfsdf\"", tokens[0].str())

        assertTrue { tokens[1] is StringLiteral }
        assertEquals("\"   \"", tokens[1].str())
    }

    @Test
    fun test2() {
        val tokens = CTokenizer.apply("4.7 /* comment */ 6")
        assertTrue { tokens[0] is Numeric }
        tokens[0].isEqual(1, 1, "4.7")

        assertTrue { tokens[1] is Numeric }
        tokens[1].isEqual(1, 19, "6")
    }

    @Test
    fun test3() {
        val tokens = CTokenizer.apply("+++")
        tokens[0].isEqual(1, 1, "++")
        tokens[1].isEqual(1, 3, "+")
    }

    @Ignore
    fun test4() {
        val tokens = CTokenizer.apply("2L")
        tokens[0].isEqual(1, 1, "2")
    }
}