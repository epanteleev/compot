import tokenizer.CTokenizer
import preprocess.Preprocesssor

import tokenizer.TokenPrinter
import kotlin.test.Test
import kotlin.test.assertEquals


class PreprocessorTest {
    @Test
    fun test1() {
        val tokens = CTokenizer.apply("#define HEAD 34\n HEAD")
        val p = Preprocesssor(tokens.toMutableList()).preprocess()
        val expected = """
            |
            |  34
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }
}