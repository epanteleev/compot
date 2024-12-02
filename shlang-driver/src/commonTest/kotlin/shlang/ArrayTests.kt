package shlang

import common.CommonCTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals


abstract class ArrayTests: CommonCTest() {
    @Test
    fun test1() {
        val result = runCTest("shlang/array/array1", listOf(), options())
        assertEquals("Array: (10, 20)\n", result.output)
    }

    @Test
    fun test2() {
        val result = runCTest("shlang/array/array2", listOf(), options())
        assertEquals("{1, 2, 3}\n", result.output)
    }

    @Test
    fun test3() {
        val result = runCTest("shlang/array/array3", listOf(), options())
        assertEquals("{1, 20, 3}\n", result.output)
    }

    @Test
    fun test4() {
        val result = runCTest("shlang/array/array4", listOf(), options())
        assertEquals("len=3 {1, 2, 3}\n", result.output)
    }

    @Test
    fun test5() {
        val result = runCTest("shlang/array/array5", listOf(), options())
        assertEquals("64\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun test6() {
        val result = runCTest("shlang/array/array6", listOf(), options())
        assertEquals("startswith\n", result.output)
        assertEquals(0, result.exitCode)
    }
}

class ArrayTestsO0: ArrayTests() {
    override fun options(): List<String> = listOf()
}

class ArrayTestsO1: ArrayTests() {
    override fun options(): List<String> = listOf("-O1")
}