import tokenizer.CTokenizer

import preprocess.*

import tokenizer.TokenPrinter
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class CProgramPreprocessorTest {
    private val testHeaderContent = """
        |#ifndef TEST_H
        |#define TEST_H
        |int a = 9;
        |#endif
    """.trimMargin()

    private val stdioHeaderContent = """
        |#ifndef STDIO_H
        |#define STDIO_H
        |int printf(char* format, ...);
        |#endif
    """.trimMargin()

    private val stdlibHeaderContent = """
        |#ifndef STDLIB_H
        |#define STDLIB_H
        |void exit(int code);
        |#endif
    """.trimMargin()

    private val headerHolder = PredefinedHeaderHolder(setOf())
        .addHeader(Header("test.h", testHeaderContent, HeaderType.USER))
        .addHeader(Header("stdio.h", stdioHeaderContent, HeaderType.SYSTEM))
        .addHeader(Header("std-32lib.h", stdlibHeaderContent, HeaderType.SYSTEM))

    @Test
    fun testEmpty() {
        val tokens = CTokenizer.apply("")
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocessWithRemovedSpaces()
        assertTrue { p.isEmpty() }
    }

    @Test
    fun testSubstitution() {
        val tokens = CTokenizer.apply("#define HEAD 34\n HEAD")
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocessWithRemovedSpaces()

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
        val p = CProgramPreprocessor.create(tokens, ctx).preprocessWithRemovedSpaces()
        val expected = """
            |
            | 34 + 34
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test
    fun testSubstitution3() {
        val input = """
            |#define check(a, b) if (a != b) { exit(1); }
            |check(sub3(9, 3, 2), 90);
        """.trimMargin()

        val tokens = CTokenizer.apply(input)
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocessWithRemovedSpaces()
        val expected = """
            |
            |if (sub3(9, 3, 2) != 90) { exit(1); };
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test
    fun testInclude() {
        val tokens = CTokenizer.apply("#include \"test.h\"")
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocessWithRemovedSpaces()
        val expected = """
            |
            |
            |int a = 9;
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test
    fun testInclude1() {
        val input = """
            |#include "test.h"
            |int a = 9;
        """.trimMargin()
        val tokens = CTokenizer.apply(input)
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocessWithRemovedSpaces()
        val expected = """
            |
            |
            |int a = 9;
            |
            |int a = 9;
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test
    fun testInclude2() {
        val tokens = CTokenizer.apply("#include \"test.h\"\n#include \"test.h\"")
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocessWithRemovedSpaces()
        val expected = """
            |
            |
            |int a = 9;
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Ignore
    fun testInclude3() {
        val tokens = CTokenizer.apply("#include <std-32lib.h>")
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocessWithRemovedSpaces()
        val expected = """
            |
            |
            |int exit(int code);
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test
    fun testIf() {
        val tokens = CTokenizer.apply("#define TEST\n#ifdef TEST\nint a = 9;\n#endif")
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocessWithRemovedSpaces()
        val expected = """
            |
            |
            |int a = 9;
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test
    fun testIf2() {
        val tokens = CTokenizer.apply("#define TEST\n#ifdef TEST\nint a = 9;\n#else\nint a = 10;\n#endif")
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocessWithRemovedSpaces()
        val expected = """
            |
            |
            |int a = 9;
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test
    fun testIf3() {
        val tokens = CTokenizer.apply("#define TEST\n#ifndef TEST\nint a = 9;\n#else\nint a = 10;\n#endif")
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocessWithRemovedSpaces()
        val expected = """
            |
            |
            |
            |
            |int a = 10;
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test
    fun testMacroFunction() {
        val tokens = CTokenizer.apply("#define SUM(a, b) a + b\nSUM(3, 4)")
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocessWithRemovedSpaces()
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
        val p = CProgramPreprocessor.create(tokens, ctx).preprocessWithRemovedSpaces()
        val expected = """
            |
            |3 + 4
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test
    fun testMacroFunction2() {
        val tokens = CTokenizer.apply("#define SUM(a, b) a + b\nSUM ( 3, 4)")
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocessWithRemovedSpaces()
        val expected = """
            |
            |3 + 4
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
        val p = CProgramPreprocessor.create(tokens, ctx).preprocessWithRemovedSpaces()
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
        val p = CProgramPreprocessor.create(tokens, ctx).preprocessWithRemovedSpaces()
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
        val p = CProgramPreprocessor.create(tokens, ctx).preprocessWithRemovedSpaces()
        val expected = """
            |
            |(0) + 1
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test
    fun testMacroFunction6() {
        val tokens = CTokenizer.apply("#define SUM(a, b) a + b\nSUM(3 + b, 4)")
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocessWithRemovedSpaces()
        val expected = """
            |
            |3 + b + 4
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
        val p = CProgramPreprocessor.create(tokens, ctx).preprocessWithRemovedSpaces()
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
        val p = CProgramPreprocessor.create(tokens, ctx).preprocessWithRemovedSpaces()
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
            |f(2 * (y+1)) + f(2 * (f(2 * (z[0])))) % f(2 * (0)) + 1;
        """.trimMargin()

        //1) t(t(g)(0) + t)(1)
        //2) t(g)(0) + t(1)
        //3) g(0) + t(1)
        //4) f(2 * (0)) + t(1)

        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test
    fun testStringify() {
        val data = """
            |#define x(a) #a
            |x(abc)
        """.trimMargin()

        val tokens = CTokenizer.apply(data)
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocessWithRemovedSpaces()
        val expected = """
            |
            |"abc"
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test
    fun testStringify1() {
        val data = """
            |#define x(a, b) #a + b
            |x(abc, 4)
        """.trimMargin()

        val tokens = CTokenizer.apply(data)
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocessWithRemovedSpaces()
        val expected = """
            |
            |"abc" + 4
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test
    fun testStringify2() {
        val data = """
            |#define x(a, b) #a + #b
            |x(abc, 4)
        """.trimMargin()

        val tokens = CTokenizer.apply(data)
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocessWithRemovedSpaces()
        val expected = """
            |
            |"abc" + "4"
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test
    fun testStringify3() {
        val data = """
            |#define x(a, b) a ## b
            |x(1, 2)
        """.trimMargin()

        val tokens = CTokenizer.apply(data)
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocessWithRemovedSpaces()
        val expected = """
            |
            |12
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test
    fun testStringify4() {
        val data = """
            |#define x(a, b, c) a ## b ## c
            |x(1, 2, 4)
        """.trimMargin()

        val tokens = CTokenizer.apply(data)
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocessWithRemovedSpaces()
        val expected = """
            |
            |124
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test
    fun testFalseStringify() {
        val data = """
            |#define x #
            |x
        """.trimMargin()

        val tokens = CTokenizer.apply(data)
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocessWithRemovedSpaces()
        val expected = """
            |
            |#
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test
    fun testFalseStringify1() {
        val data = """
            |#define x #
            |x + 2
        """.trimMargin()

        val tokens = CTokenizer.apply(data)
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocessWithRemovedSpaces()
        val expected = """
            |
            |# + 2
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test
    fun testIfdefined() {
        val data = """
            |#define TEST
            |#if defined(TEST)
            |int a = 9;
            |#endif
        """.trimMargin()

        val tokens = CTokenizer.apply(data)
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocessWithRemovedSpaces()
        val expected = """
            |
            |
            |int a = 9;
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test
    fun testIfdefined2() {
        val data = """
            |#define TEST
            |#if defined(TEST)
            |int a = 9;
            |#else
            |int a = 10;
            |#endif
        """.trimMargin()

        val tokens = CTokenizer.apply(data)
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocessWithRemovedSpaces()
        val expected = """
            |
            |
            |int a = 9;
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test
    fun testIfdefined3() {
        val data = """
            |#if defined(TEST)
            |int a = 9;
            |#else
            |int a = 10;
            |#endif
        """.trimMargin()

        val tokens = CTokenizer.apply(data)
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocessWithRemovedSpaces()
        val expected = """
            |
            |
            |
            |int a = 10;
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test
    fun testIfdefined4() {
        val data = """
            |#define TEST
            |#if !defined(TEST)
            |int a = 9;
            |#elif defined(TEST)
            |int a = 6;
            |#else
            |int a = 10;
            |#endif
        """.trimMargin()

        val tokens = CTokenizer.apply(data)
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocessWithRemovedSpaces()
        val expected = """
            |
            |
            |
            |
            |int a = 6;
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test
    fun testIfdefined5() {
        val data = """
            |#define TEST
            |#if defined (TEST)
            |int a = 9;
            |#else
            |int a = 10;
            |#endif
        """.trimMargin()

        val tokens = CTokenizer.apply(data)
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocessWithRemovedSpaces()
        val expected = """
            |
            |
            |int a = 9;
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test
    fun testIfdefined6() {
        val data = """
            |# define TEST
            |#if defined (TEST)
            |int a = 9;
            |# else
            |int a = 10;
            |# endif
        """.trimMargin()

        val tokens = CTokenizer.apply(data)
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocessWithRemovedSpaces()
        val expected = """
            |
            |
            |int a = 9;
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test
    fun testSeveralIf() {
        val data = """
            |#ifndef TEST
            |#define TEST
            |#endif
            |
            |#if !defined(TEST)
            |int a = 9;
            |#elif defined(TEST)
            |int a = 6;
            |#else
            |int a = 10;
            |#endif
        """.trimMargin()

        val tokens = CTokenizer.apply(data)
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocessWithRemovedSpaces()
        val expected = """
            |
            |
            |
            |
            |
            |
            |
            |int a = 6;
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test
    fun testPredefinedMacros() {
        val data = """
            |__LINE__
        """.trimMargin()

        val tokens = CTokenizer.apply(data)
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocessWithRemovedSpaces()
        val expected = """
            |1
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test                                           //TODO not fully correct test
    fun testPredefinedMacros2() {
        val data = """
            |__FILE__
        """.trimMargin()

        val tokens = CTokenizer.apply(data)
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx).preprocessWithRemovedSpaces()
        val expected = """
            |"<no-name>"
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p))
    }

    @Test
    fun testError() {
        val data = """
            |#error Error message
        """.trimMargin()

        val tokens = CTokenizer.apply(data)
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx)
        try {
            p.preprocessWithRemovedSpaces()
        } catch (e: PreprocessorException) {
            assertEquals("#error Error message", e.message)
        }
    }

    @Test
    fun testSysInclude() {
        val data = """
            |#include <stdio.h>
        """.trimMargin()

        val tokens = CTokenizer.apply(data)
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx)
        val expected = """
            |
            |
            |int printf(char* format, ...);
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p.preprocessWithRemovedSpaces()))
    }

    // 6.10.2 Source file inclusion
    // EXAMPLE 2
    @Test
    fun testSysInclude2() {
        val data = """
            |#if VERSION == 1
            |#define INCFILE            "vers1.h"
            |#elif VERSION == 2
            |#define INCFILE            "vers2.h"
            |#else
            |#define INCFILE           "test.h"
            |#endif
            |#include INCFILE
            |int aa = 90;
        """.trimMargin()

        val tokens = CTokenizer.apply(data)
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx)
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
            |int a = 9;
            |
            |int aa = 90;
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p.preprocessWithRemovedSpaces()))
    }

    @Test
    fun testSysInclude3() {
        val data = """
            |#define  HEADER(name) #name ".h"
            |#include "stdio.h"
        """.trimMargin()

        val tokens = CTokenizer.apply(data)
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx)
        val expected = """
            |
            |
            |
            |int printf(char* format, ...);
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p.preprocessWithRemovedSpaces()))
    }

    @Test
    fun testDefined() {
        val data = """
            |#define TEST
            |#if defined TEST
            |int a = 9;
            |#endif
        """.trimMargin()

        val tokens = CTokenizer.apply(data)
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx)
        val expected = """
            |
            |
            |int a = 9;
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p.preprocessWithRemovedSpaces()))
    }

    @Test
    fun testDefined2() {
        val data = """
            |#define TEST1
            |#if (defined TEST1	\
            |            || defined TEST2)
            |int a = 9;
            |#endif
        """.trimMargin()

        val tokens = CTokenizer.apply(data)
        val ctx = PreprocessorContext.empty(headerHolder)
        val p = CProgramPreprocessor.create(tokens, ctx)
        val expected = """
            |
            |
            |int a = 9;
        """.trimMargin()
        assertEquals(expected, TokenPrinter.print(p.preprocessWithRemovedSpaces()))
    }
}