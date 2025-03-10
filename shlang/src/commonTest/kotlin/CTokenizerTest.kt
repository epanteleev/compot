
import tokenizer.*
import kotlin.test.*
import tokenizer.tokens.*
import kotlin.test.assertTrue
import kotlin.test.assertEquals


class CTokenizerTest {
    private fun TokenList.toCTokenList(): MutableList<AnyToken> {
        val result = mutableListOf<AnyToken>()
        for (token in this) {
            if (token is AnySpaceToken) {
                continue
            }

            result.add(token)
        }
        return result
    }

    private fun apply(input: String): TokenList {
        return CTokenizer.apply(input, "<test-data>")
    }

    private fun AnyToken.isEqual(l: Int, p: Int, string: String) {
        this as CToken
        assertEquals(l, line())
        assertEquals(p, pos())
        assertEquals(string, str())
    }

    @Test
    fun test0() {
        val tokens = apply("4566,").toCTokenList()

        assertTrue { tokens[0] is PPNumber }
        tokens[0].isEqual(1, 1, "4566")
        tokens[1].isEqual(1, 5, ",")
    }

    @Test
    fun test1() {
        val tokens = apply("\"sdfsdf\" \"   \"").toCTokenList()

        assertTrue { tokens[0] is StringLiteral }
        assertEquals("\"sdfsdf\"", tokens[0].str())
    }

    @Test
    fun test2() {
        val tokens = apply("4.7 /* comment */ 6").toCTokenList()
        assertTrue { tokens[0] is PPNumber }
        tokens[0].isEqual(1, 1, "4.7")

        assertTrue { tokens[1] is PPNumber }
        tokens[1].isEqual(1, 19, "6")
    }

    @Test
    fun test3() {
        val tokens = apply("+++").toCTokenList()
        tokens[0].isEqual(1, 1, "++")
        tokens[1].isEqual(1, 3, "+")
    }

    @Test
    fun test4() {
        val tokens = apply("2L").toCTokenList()
        tokens[0].isEqual(1, 1, "2L")
    }

    @Test
    fun test5() {
        val tokens = apply("int x = -1; // comment").toCTokenList()
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
        val tokens = apply(input).toCTokenList()
        assertTrue { tokens[0] is PPNumber }
        tokens[0].isEqual(1, 1, "4.7")

        assertTrue { tokens[1] is PPNumber }
        tokens[1].isEqual(2, 8, "6")
    }

    @Test
    fun test7() {
        val input = "0x88"
        val tokens = apply(input).toCTokenList()
        assertTrue { tokens[0] is PPNumber }
        tokens[0].isEqual(1, 1, "0x88") //TODO print hex??
    }

    @Test
    fun test8() {
        val input = "# define UINT_LEAST64_MAX\t(__UINT64_C(18446744073709551615))"
        val tokens = apply(input).toCTokenList()
        tokens[6].isEqual(1, 39, "18446744073709551615") //TODO print hex??
    }

    @Test
    fun test9() {
        val input = "0x88ull"
        val tokens = apply(input).toCTokenList()
        assertTrue { tokens[0] is PPNumber }
        tokens[0].isEqual(1, 1, "0x88ull")
    }

    @Test
    fun test10() {
        val input = "0xff"
        val tokens = apply(input).toCTokenList()
        assertTrue { tokens[0] is PPNumber }
        tokens[0].isEqual(1, 1, "0xff")
        val tok = tokens[0] as PPNumber
        assertEquals(255, tok.toNumberOrNull())
    }

    @Test
    fun testZeroFloatLiteral0() {
        val input = "0."
        val tokens = apply(input).toCTokenList()
        assertTrue { tokens[0] is PPNumber }
        tokens[0].isEqual(1, 1, "0.")
    }

    @Test
    fun testZeroFloatLiteral1() {
        val input = "0.f"
        val tokens = apply(input).toCTokenList()
        assertTrue { tokens[0] is PPNumber }
        assertEquals(1, tokens.size)
        tokens[0].isEqual(1, 1, "0.f")
    }

    @Test
    fun testZeroFloatLiteral2() {
        val input = "00000000l"
        val tokens = apply(input).toCTokenList()
        assertTrue { tokens[0] is PPNumber }
        assertEquals(1, tokens.size)
        val num = tokens[0] as PPNumber
        assertEquals(0L, num.toNumberOrNull().toLong())
    }

    @Test
    fun testExponent() {
        val input = "45e14"
        val tokens = apply(input).toCTokenList()
        assertTrue { tokens[0] is PPNumber }
        tokens[0].isEqual(1, 1, "45e14")
        val num = tokens[0] as PPNumber
        assertEquals(4500000000000000.000000, num.toNumberOrNull())
    }

    @Test
    fun testExponent1() {
        val input = "1e-9"
        val tokens = apply(input).toCTokenList()
        assertTrue { tokens[0] is PPNumber }
        tokens[0].isEqual(1, 1, "1e-9")
        val num = tokens[0] as PPNumber
        assertEquals(1.0E-9, num.toNumberOrNull())
    }

    @Test
    fun testPower() {
        val input = "0x1.ffffp+3"
        val tokens = apply(input).toCTokenList()
        assertTrue { tokens[0] is PPNumber }
        tokens[0].isEqual(1, 1, "0x1.ffffp+3")
        val num = tokens[0] as PPNumber
        assertEquals(15.9998779296875, num.toNumberOrNull())
    }

    @Test
    fun testLongLiteral() {
        val input = "0xffff000000000000LL"
        val tokens = apply(input).toCTokenList()
        assertTrue { tokens[0] is PPNumber }
        tokens[0].isEqual(1, 1, "0xffff000000000000LL")
        val num = tokens[0] as PPNumber
        assertEquals("FFFF000000000000".toULong(16).toLong(), num.toNumberOrNull())
    }

    @Test
    fun testQuotedString() {
        val input = "\"\\\"Hello, World!\\\"\""
        val tokens = apply(input).toCTokenList()
        assertTrue { tokens[0] is StringLiteral }
        tokens[0].isEqual(1, 1, "\"\\\"Hello, World!\\\"\"")
    }

    @Test
    fun testCharLiteral() {
        val input = "'\\x3b'"
        val tokens = apply(input).toCTokenList()
        assertTrue { tokens[0] is CharLiteral }
        tokens[0].isEqual(1, 1, "';'")
    }

    @Test
    fun testLibPngBug() {
        val input = "-2147483647.)"
        val tokens = apply(input).toCTokenList()
        assertTrue { tokens[1] is PPNumber }
        tokens[1].isEqual(1, 2, "2147483647.")
    }

    @Test
    fun testBinaryLiteral() {
        val input = "0b101010"
        val tokens = apply(input).toCTokenList()
        assertTrue { tokens[0] is PPNumber }
        tokens[0].isEqual(1, 1, "0b101010")
        val num = tokens[0] as PPNumber
        assertEquals("42".toByte(), num.toNumberOrNull())
    }

    @Test
    fun testHexChar() {
        val input = "'\\xef'"
        val tokens = apply(input).toCTokenList()
        assertTrue { tokens[0] is CharLiteral }
        val literal = tokens[0] as CharLiteral
        assertEquals(-17, literal.code())
        tokens[0].isEqual(1, 1, "'ï'")
    }

    @Test
    fun testStringLiteralWithEscapeChar() {
        val input = "\"\\46ELF\""
        val tokens = apply(input).toCTokenList()
        assertTrue { tokens[0] is StringLiteral }
        tokens[0].isEqual(1, 1, "\"&ELF\"")
    }
}