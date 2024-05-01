import tokenizer.CTokenizer
import preprocess.CProgramPreprocessor

import tokenizer.TokenPrinter
import kotlin.test.Test
import kotlin.test.assertEquals


class CProgramPreprocessorTest {
    @Test
    fun testSubstitution() {
        val tokens = CTokenizer.apply("#define HEAD 34\n HEAD")
        val p = CProgramPreprocessor(tokens).preprocess()
        val expected = """
            |
            | 34
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test
    fun testSubstitution2() {
        val tokens = CTokenizer.apply("#define HEAD 34\n HEAD + HEAD")
        val p = CProgramPreprocessor(tokens).preprocess()
        val expected = """
            |
            | 34 + 34
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }
}