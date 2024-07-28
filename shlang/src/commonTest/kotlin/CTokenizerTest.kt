
import tokenizer.*
import kotlin.test.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class CTokenizerTest {
    fun TokenList.toCTokenList(): MutableList<AnyToken> {
        val result = mutableListOf<AnyToken>()
        for (token in this) {
            if (token is AnySpaceToken) {
                continue
            }

            result.add(token)
        }
        return result
    }

    fun AnyToken.isEqual(l: Int, p: Int, string: String) {
        this as CToken
        assertEquals(l, line())
        assertEquals(p, pos())
        assertEquals(string, str())
    }

    @Test
    fun test0() {
        val tokens = CTokenizer.apply("4566,").toCTokenList()

        assertTrue { tokens[0] is Numeric }
        tokens[0].isEqual(1, 1, "4566")
        tokens[1].isEqual(1, 5, ",")
    }

    @Test
    fun test1() {
        val tokens = CTokenizer.apply("\"sdfsdf\" \"   \"").toCTokenList()

        assertTrue { tokens[0] is StringLiteral }
        assertEquals("\"sdfsdf   \"", tokens[0].str())
    }

    @Test
    fun test2() {
        val tokens = CTokenizer.apply("4.7 /* comment */ 6").toCTokenList()
        assertTrue { tokens[0] is Numeric }
        tokens[0].isEqual(1, 1, "4.7")

        assertTrue { tokens[1] is Numeric }
        tokens[1].isEqual(1, 19, "6")
    }

    @Test
    fun test3() {
        val tokens = CTokenizer.apply("+++").toCTokenList()
        tokens[0].isEqual(1, 1, "++")
        tokens[1].isEqual(1, 3, "+")
    }

    @Test
    fun test4() {
        val tokens = CTokenizer.apply("2L").toCTokenList()
        tokens[0].isEqual(1, 1, "2L")
    }

    @Test
    fun test5() {
        val tokens = CTokenizer.apply("int x = -1; // comment").toCTokenList()
        assertEquals(6, tokens.size)
        tokens[0].isEqual(1, 1, "int")
        tokens[1].isEqual(1, 5, "x")
        tokens[2].isEqual(1, 7, "=")
        tokens[3].isEqual(1, 9, "-")
        tokens[4].isEqual(1, 10, "1")
        tokens[5].isEqual(1, 11, ";")
    }

    @Test
    fun test6() {
        val input = """
            4.7 /* comment
            test */ 6
        """.trimIndent()
        val tokens = CTokenizer.apply(input).toCTokenList()
        assertTrue { tokens[0] is Numeric }
        tokens[0].isEqual(1, 1, "4.7")

        assertTrue { tokens[1] is Numeric }
        tokens[1].isEqual(2, 8, "6")
    }

    @Test
    fun test7() {
        val input = "0x88"
        val tokens = CTokenizer.apply(input).toCTokenList()
        assertTrue { tokens[0] is Numeric }
        tokens[0].isEqual(1, 1, "88") //TODO print hex??
    }

    @Ignore
    fun test8() {
        val input = "# define UINT_LEAST64_MAX\t(__UINT64_C(18446744073709551615))"
        val tokens = CTokenizer.apply(input).toCTokenList()
        assertTrue { tokens[0] is Numeric }
        tokens[0].isEqual(1, 1, "136") //TODO print hex??
    }

    @Test
    fun test9() {
        val input = "0x88ull"
        val tokens = CTokenizer.apply(input).toCTokenList()
        assertTrue { tokens[0] is Numeric }
        tokens[0].isEqual(1, 1, "88ull")
    }

    @Test
    fun test10() {
        val input = "0xff"
        val tokens = CTokenizer.apply(input).toCTokenList()
        assertTrue { tokens[0] is Numeric }
        tokens[0].isEqual(1, 1, "ff")
        val tok = tokens[0] as Numeric
        assertEquals(255, tok.toNumberOrNull())
    }

    @Test
    fun testStringConcatenation() {
        val input = "\"Hello\" \"World\""
        val tokens = CTokenizer.apply(input).toCTokenList()
        assertTrue { tokens[0] is StringLiteral }
        tokens[0].isEqual(1, 1, "\"HelloWorld\"")
    }

    @Test
    fun testZeroFloatLiteral0() {
        val input = "0."
        val tokens = CTokenizer.apply(input).toCTokenList()
        assertTrue { tokens[0] is Numeric }
        tokens[0].isEqual(1, 1, "0.")
    }

    @Test
    fun testZeroFloatLiteral1() {
        val input = "0.f"
        val tokens = CTokenizer.apply(input).toCTokenList()
        assertTrue { tokens[0] is Numeric }
        assertEquals(1, tokens.size)
        tokens[0].isEqual(1, 1, "0.f")
    }
}