package shlang

import common.CommonCTest
import kotlin.test.Test
import kotlin.test.assertEquals


sealed class VarArgsTests: CommonCTest() {
    @Test
    fun test1() {
        val result = runCTest("shlang/varArgs/varArgs1", listOf(), options())
        assertEquals("Number: 1\n", result.output)
    }

    @Test
    fun test2() {
        val result = runCTest("shlang/varArgs/varArgs2", listOf(), options())
        assertEquals("Number: 1\n", result.output)
        assertEquals(88, result.exitCode)
    }

    @Test
    fun test3() {
        val result = runCTest("shlang/varArgs/varArgs3", listOf(), options())
        assertEquals("Numbers: 1 2 3 4 5 6\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun test4() {
        val result = runCTest("shlang/varArgs/varArgs4", listOf(), options())
        assertEquals("Numbers: 1 2\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun test5() {
        val result = runCTest("shlang/varArgs/varArgs5", listOf(), options())
        assertEquals("Number: 1.000000\n", result.output)
    }

    @Test
    fun test6() {
        val result = runCTest("shlang/varArgs/varArgs6", listOf(), options())
        assertEquals("Number: 1.000000\n", result.output)
    }

    @Test
    fun test7() {
        val result = runCTest("shlang/varArgs/varArgs7", listOf(), options())
        assertEquals("Point: (1, 2)\n", result.output)
    }

    @Test
    fun test8() {
        val result = runCTest("shlang/varArgs/varArgs8", listOf(), options())
        assertEquals("Vector: (1, 2, 3)\n", result.output)
    }

    @Test
    fun test9() {
        val result = runCTest("shlang/varArgs/varArgs9", listOf(), options())
        assertEquals("Point: (1, 2)\n", result.output)
    }

    @Test
    fun test10() {
        val result = runCTest("shlang/varArgs/varArgs10", listOf(), options())
        assertEquals("Point: (1.000000, 2.000000)\n", result.output)
    }

    @Test
    fun test11() {
        val result = runCTest("shlang/varArgs/varArgs11", listOf(), options())
        assertEquals("Point: (1, 2)\n", result.output)
    }

    @Test
    fun test12() {
        val result = runCTest("shlang/varArgs/varArgs12", listOf(), options())
        assertEquals("Point: (1, 2), Vector: (3, 4, 5)\n", result.output)
    }

    @Test
    fun test13() {
        val result = runCTest("shlang/varArgs/varArgs13", listOf(), options())
        assertEquals("my_printf0: Point: (1, 2), Vector: (3, 4, 5)\n", result.output)
    }

    @Test
    fun test14() {
        val result = runCTest("shlang/varArgs/varArgs14", listOf(), options())
        assertEquals("print Hello\n", result.output)
    }

    @Test
    fun test15() {
        val result = runCTest("shlang/varArgs/varArgs15", listOf(), options())
        assertEquals("x: 1 y: 2 z: 3 w: 4 v: 5\n", result.output)
    }

    @Test
    fun test16() {
        val result = runCTest("shlang/varArgs/varArgs16", listOf(), options())
        assertEquals("Hello, world!\n", result.output)
    }

    @Test
    fun test17() {
        val result = runCTest("shlang/varArgs/varArgs17", listOf(), options())
        assertEquals("variadic: 1 2 3 4 5 6\nNumbers: 7 8 9\n", result.output)
    }

    @Test
    fun test18() {
        val result = runCTest("shlang/varArgs/varArgs18", listOf(), options())
        assertEquals("variadic: 1 2 3 4\nNumbers: 6 7 8 9\n", result.output)
    }
}

class VarArgsTestsO0: VarArgsTests() {
    override fun options(): List<String> = listOf()
}

class VarArgsTestsO1: VarArgsTests() {
    override fun options(): List<String> = listOf("-O1")
}