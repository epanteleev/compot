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
}

class StdlibTestO0: StdlibTest() {
    override fun options(): List<String> = listOf()
}

class StdlibTestO1: StdlibTest() {
    override fun options(): List<String> = listOf("-O1")
}