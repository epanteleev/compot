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
}

class StdlibTestO0: StdlibTest() {
    override fun options(): List<String> = listOf()
}

class StdlibTestO1: StdlibTest() {
    override fun options(): List<String> = listOf("-O1")
}