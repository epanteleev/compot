package compot

import common.CommonCTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals


abstract class ArrayTests: CommonCTest() {
    @Test
    fun test1() {
        val result = runCTest("compot/array/array1", listOf(), options())
        assertEquals("Array: (10, 20)\n", result.output)
    }

    @Test
    fun test2() {
        val result = runCTest("compot/array/array2", listOf(), options())
        assertEquals("{1, 2, 3}\n", result.output)
    }

    @Test
    fun test3() {
        val result = runCTest("compot/array/array3", listOf(), options())
        assertEquals("{1, 20, 3}\n", result.output)
    }

    @Test
    fun test4() {
        val result = runCTest("compot/array/array4", listOf(), options())
        assertEquals("len=3 {1, 2, 3}\n", result.output)
    }

    @Test
    fun test5() {
        val result = runCTest("compot/array/array5", listOf(), options())
        assertEquals("64\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun test6() {
        val result = runCTest("compot/array/array6", listOf(), options())
        assertEquals("startswith\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun test7() {
        val result = runCTest("compot/array/array7", listOf(), options())
        assertEquals("/\n", result.output)
    }

    @Test
    fun test8() {
        val result = runCTest("compot/array/array8", listOf(), options())
        assertEquals("Array is zeroed\n", result.output)
    }

    @Test
    fun test9() {
        val result = runCTest("compot/array/array9", listOf(), options())
        assertEquals("len: 0, capacity: 1\n", result.output)
    }

    @Test
    fun test10() {
        val result = runCTest("compot/array/array10", listOf(), options())
        assertEquals("gammas: 2.200000 1.000000 1.517241 1.800000 1.500000 2.400000 2.500000 2.620000 2.900000\n", result.output)
    }

    @Test
    fun test11() {
        val result = runCTest("compot/array/array11", listOf(), options())
        assertEquals("cof cob cif cib rof rob\n", result.output)
    }
}

class ArrayTestsO0: ArrayTests() {
    override fun options(): List<String> = listOf()
}

class ArrayTestsO1: ArrayTests() {
    override fun options(): List<String> = listOf("-O1")
}