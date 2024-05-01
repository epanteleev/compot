import tokenizer.CTokenizer
import preprocess.CProgramPreprocessor
import preprocess.Header
import preprocess.PredefinedHeaderHolder
import preprocess.PreprocessorContext

import tokenizer.TokenPrinter
import kotlin.test.BeforeTest
import kotlin.test.Ignore
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

    @Test
    fun testMacroFunction() {
        val tokens = CTokenizer.apply("#define SUM(a, b) a + b\nSUM(3, 4)")
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocess()
        val expected = """
            |
            |3 + 4
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test
    fun testMacroFunction1() {
        val tokens = CTokenizer.apply("#define SUM(a, b) a + b\nSUM ( 3, 4)")
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocess()
        val expected = """
            |
            |3 + 4
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test
    fun testMacroFunction2() {
        val tokens = CTokenizer.apply("#define SUM(a, b) a + b\nSUM(3, 4) + SUM(5, 6)")
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocess()
        val expected = """
            |
            |3 + 4 + 5 + 6
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test
    fun testMacroFunction3() {
        val data = """
            |#define a 7
            |#define SUM(a, b) a + b
            |SUM(a, 4) + a
        """.trimMargin()

        val tokens = CTokenizer.apply(data)
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocess()
        val expected = """
            |
            |
            |7 + 4 + 7
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }


    @Test
    fun testMacroFunction4() {
        val tokens = CTokenizer.apply("#define INC(a) a + 1\nINC(0)")
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocess()
        val expected = """
            |
            |0 + 1
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test
    fun testMacroFunction5() {
        val tokens = CTokenizer.apply("#define INC(a) a + 1\nINC((0))")
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocess()
        val expected = """
            |
            |(0) + 1
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    // 6.10.3.4 Rescanning and further replacement
    // EXAMPLE
    @Test
    fun testRecursiveExpansion() {
        val data = """
            |#define f(a) a*g
            |#define g(a) f(a)
            |f(2)(9)
        """.trimMargin()

        val tokens = CTokenizer.apply(data)
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocess()
        val expected = """
            |
            |
            |2*9*g
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    // 6.10.3.5 Scope of macro definitions
    // EXAMPLE 3
    @Test //TODO not fully correct test
    fun testMacroFunction444() {
        val data = """
            |#define    x          3
            |#define    f(a)       f(x * (a))
            |#undef     x
            |#define    x          2
            |#define    g          f
            |#define    z          z[0]
            |#define    h          g(~
            |#define    m(a)       a(w)
            |#define    w          0,1
            |#define    t(a)       a
            |#define    p()        int
            |#define    q(x)       x
            |f(y+1) + f(f(z)) % t(t(g)(0) + t)(1);
        """.trimMargin()

        val tokens = CTokenizer.apply(data)
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocess()
        val expected = """
            |
            |
            |
            |
            |
            |
            |
            |
            |
            |
            |
            |
            |f(2 * (y+1)) + f(2 * (f(2 * (z[0])))) % f(2 * (0))+1;
        """.trimMargin()

        //1) t(t(g)(0) + t)(1)
        //2) t(g)(0) + t(1)
        //3) g(0) + t(1)
        //4) f(2 * (0)) + t(1)

        assertEquals(expected, TokenPrinter.print(p))
    }
}