package shlang

import common.CommonCTest
import kotlin.test.Test
import kotlin.test.assertEquals


abstract class ForLoopTests: CommonCTest() {
    @Test
    fun testDoWhile() {
        val result = runCTest("shlang/loop/doWhile", listOf(), options())
        assertReturnCode(result, 20)
    }

    @Test
    fun testForLoop() {
        val result = runCTest("shlang/loop/forLoop", listOf("runtime/runtime.c"), options())
        assertEquals(30, result.exitCode)
    }

    @Test
    fun testForLoop0() {
        val result = runCTest("shlang/loop/forLoop0", listOf("runtime/runtime.c"), options())
        assert(result, "Done")
    }

    @Test
    fun testForLoop1() {
        val result = runCTest("shlang/loop/forLoop1", listOf("runtime/runtime.c"), options())
        assert(result, "Done")
    }

    @Test
    fun testForLoop2() {
        val result = runCTest("shlang/loop/forLoop2", listOf("runtime/runtime.c"), options())
        assertEquals(30, result.exitCode)
    }

    @Test
    fun testForLoop3() {
        val result = runCTest("shlang/loop/forLoop3", listOf("runtime/runtime.c"), options())
        assertEquals(20, result.exitCode)
    }

    @Test
    fun testMemset1() {
        val result = runCTest("shlang/loop/memset1", listOf("runtime/runtime.c"), options())
        assert(result, "0 0 0 0 0 0 0 0 0 0 \n")
    }
}

class ForLoopTestsO0: ForLoopTests() {
    override fun options(): List<String> = listOf()
}

class ForLoopTestsO1: ForLoopTests() {
    override fun options(): List<String> = listOf("-O1")
}