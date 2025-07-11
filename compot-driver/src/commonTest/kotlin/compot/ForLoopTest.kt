package compot

import common.CommonCTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals


abstract class ForLoopTests: CommonCTest() {
    @Test
    fun testDoWhile() {
        val result = runCTest("compot/loop/doWhile", listOf(), options())
        assertReturnCode(result, 20)
    }

    @Test
    fun testForLoop() {
        val result = runCTest("compot/loop/forLoop", listOf("runtime/runtime.c"), options())
        assertEquals(30, result.exitCode)
    }

    @Test
    fun testForLoop0() {
        val result = runCTest("compot/loop/forLoop0", listOf("runtime/runtime.c"), options())
        assert(result, "Done")
    }

    @Test
    fun testForLoop1() {
        val result = runCTest("compot/loop/forLoop1", listOf("runtime/runtime.c"), options())
        assert(result, "Done")
    }

    @Test
    fun testForLoop2() {
        val result = runCTest("compot/loop/forLoop2", listOf("runtime/runtime.c"), options())
        assertEquals(30, result.exitCode)
    }

    @Test
    fun testForLoop3() {
        val result = runCTest("compot/loop/forLoop3", listOf("runtime/runtime.c"), options())
        assertEquals("Done", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testMemset1() {
        val result = runCTest("compot/loop/memset1", listOf("runtime/runtime.c"), options())
        assert(result, "0 0 0 0 0 0 0 0 0 0 \n")
    }

    @Test
    fun testGotoLoop1() {
        val result = runCTest("compot/loop/gotoLoop1", listOf("runtime/runtime.c"), options())
        assertEquals(10, result.exitCode)
    }
}

class ForLoopTestsO0: ForLoopTests() {
    override fun options(): List<String> = listOf()
}

class ForLoopTestsO1: ForLoopTests() {
    override fun options(): List<String> = listOf("-O1")
}