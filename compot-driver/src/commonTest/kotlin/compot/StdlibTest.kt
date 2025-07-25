package compot

import common.CommonCTest
import org.junit.Test
import kotlin.test.assertEquals


sealed class StdlibTest: CommonCTest() {
    @Test
    fun test1() {
        val result = runCTest("compot/stdlib/isdigit", listOf(), options())
        assertEquals("isdigit\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testAlignedAlloc() {
        val result = runCTest("compot/stdlib/aligned_alloc", listOf(), options())
        assertEquals("Done\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testConcatStr() {
        val result = runCTest("compot/stdlib/concat_str", listOf(), options())
        assertEquals("a=0; (function(x){a=x;})('hi'); a \"hi\"", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testStdio() {
        val result = runCTest("compot/stdlib/stdio", listOf(), options())
        assertEquals("done\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testTernary() {
        val result = runCTest("compot/stdlib/ternary", listOf(), options())
        assertEquals("Hello\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testString() {
        val result = runCTest("compot/stdlib/string", listOf(), options())
        assertEquals("11000010 10000000", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testString1() {
        val result = runCTest("compot/stdlib/string1", listOf(), options())
        assertEquals("11010000 10010111", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testString2() {
        val result = runCTest("compot/stdlib/string2", listOf(), options())
        assertEquals("🚩😁", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testString3() {
        val result = runCTest("compot/stdlib/ulong", listOf(), options())
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testMaxSize() {
        val result = runCTest("compot/stdlib/maxsize", listOf(), options())
        assertEquals("v: 1908874353", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testStrlen() {
        val result = runCTest("compot/stdlib/strlen", listOf(), options())
        assertEquals("Hello, world!\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testPrintStr() {
        val result = runCTest("compot/stdlib/print_str", listOf(), options())
        assertEquals("Hello, world!\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testErrno() {
        val result = runCTest("compot/stdlib/errno", listOf(), options())
        assertEquals("errno: EPERM\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testGlob() {
        val result = runCTest("compot/stdlib/glob", listOf(), options())
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testMemsetArg() {
        val result = runCTest("compot/stdlib/memset_arg", listOf(), options())
        assertEquals("Date: 1/1/1\n", result.output)
        assertEquals(0, result.exitCode)
    }
}

class StdlibTestO0: StdlibTest() {
    override fun options(): List<String> = listOf()
}

class StdlibTestO1: StdlibTest() {
    override fun options(): List<String> = listOf("-O1")
}

class StdlibTestO1fPIC: StdlibTest() {
    override fun options(): List<String> = listOf("-O1", "-fPIC")
}