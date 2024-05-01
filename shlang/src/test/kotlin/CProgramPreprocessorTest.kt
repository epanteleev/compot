import tokenizer.CTokenizer
import preprocess.CProgramPreprocessor
import preprocess.Header
import preprocess.PredefinedHeaderHolder
import preprocess.PreprocessorContext

import tokenizer.TokenPrinter
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals


class CProgramPreprocessorTest {
    private val testHeaderContent = """
        |#ifndef TEST_H
        |#define TEST_H
        |int a = 9;
        |#endif
    """.trimMargin()

    private val headerHolder = PredefinedHeaderHolder(setOf())
        .addHeader(Header("test.h", testHeaderContent))

    @Test
    fun testSubstitution() {
        val tokens = CTokenizer.apply("#define HEAD 34\n HEAD")
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocess()

        val expected = """
            |
            | 34
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test
    fun testSubstitution2() {
        val tokens = CTokenizer.apply("#define HEAD 34\n HEAD + HEAD")
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocess()
        val expected = """
            |
            | 34 + 34
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test
    fun testInclude() {
        val tokens = CTokenizer.apply("#include \"test.h\"")
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocess()
        val expected = """
            |
            |
            |int a = 9;
            |
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test
    fun testInclude2() {
        val tokens = CTokenizer.apply("#include \"test.h\"\n#include \"test.h\"")
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocess()
        val expected = """
            |
            |
            |int a = 9;
            |
            |
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }
    @Test
    fun testIf() {
        val tokens = CTokenizer.apply("#define TEST\n#ifdef TEST\nint a = 9;\n#endif")
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocess()
        val expected = """
            |
            |
            |int a = 9;
            |
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test
    fun testIf2() {
        val tokens = CTokenizer.apply("#define TEST\n#ifdef TEST\nint a = 9;\n#else\nint a = 10;\n#endif")
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocess()
        val expected = """
            |
            |
            |int a = 9;
            |
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test
    fun testIf3() {
        val tokens = CTokenizer.apply("#define TEST\n#ifndef TEST\nint a = 9;\n#else\nint a = 10;\n#endif")
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocess()
        val expected = """
            |
            |
            |int a = 10;
            |
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }
}