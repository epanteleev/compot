package shlang

import common.CommonCTest
import org.junit.Test
import kotlin.test.assertEquals


sealed class StdlibTest: CommonCTest() {
    @Test
    fun test1() {
        val result = runCTest("shlang/stdlib/isdigit", listOf(), options())
        assertEquals("isdigit\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testAlignedAlloc() {
        val result = runCTest("shlang/stdlib/aligned_alloc", listOf(), options())
        assertEquals("Done\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testConcatStr() {
        val result = runCTest("shlang/stdlib/concat_str", listOf(), options())
        assertEquals("a=0; (function(x){a=x;})('hi'); a \"hi\"", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testStdio() {
        val result = runCTest("shlang/stdlib/stdio", listOf(), options())
        assertEquals("done\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testTernary() {
        val result = runCTest("shlang/stdlib/ternary", listOf(), options())
        assertEquals("Hello\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testString() {
        val result = runCTest("shlang/stdlib/string", listOf(), options())
        assertEquals("11000010 10000000", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testString1() {
        val result = runCTest("shlang/stdlib/string1", listOf(), options())
        assertEquals("11010000 10010111", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testString2() {
        val result = runCTest("shlang/stdlib/string2", listOf(), options())
        assertEquals("🚩😁", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testString3() {
        val result = runCTest("shlang/stdlib/ulong", listOf(), options())
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testMaxSize() {
        val result = runCTest("shlang/stdlib/maxsize", listOf(), options())
        assertEquals("v: 1908874353", result.output)
        assertEquals(0, result.exitCode)
    }
}

class StdlibTestO0: StdlibTest() {
    override fun options(): List<String> = listOf()
}

class StdlibTestO1: StdlibTest() {
    override fun options(): List<String> = listOf("-O1")
}