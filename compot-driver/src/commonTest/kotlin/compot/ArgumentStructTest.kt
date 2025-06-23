package compot

import common.CommonCTest
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class ArgumentStructTest: CommonCTest() {
    @Test
    fun testArgumentStruct() {
        val result = runCTest("compot/argument_struct/argument_struct", listOf("runtime/runtime.c"), options())
        assertEquals("x: 1, y: 2\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testArgumentStruct1() {
        val result = runCTest("compot/argument_struct/argument_struct1", listOf(), options())
        assertEquals("x: 1, y: 2, z: 3\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testArgumentStruct2() {
        val result = runCTest("compot/argument_struct/argument_struct2", listOf(), options())
        assertEquals("x: 1, y: 2, z: 3\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testArgumentStruct3() {
        val result = runCTest("compot/argument_struct/argument_struct3", listOf(), options())
        assertEquals("x: 1 y: 2 z: 3 w: 4 v: 5 u: 6 t: 7 s: 8\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testArgumentStruct4() {
        val result = runCTest("compot/argument_struct/argument_struct4", listOf(), options())
        assertEquals("x: 1, y: 2, z: 3\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testArgumentStruct5() {
        val result = runCTest("compot/argument_struct/argument_struct5", listOf(), options())
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testArgumentStruct6() {
        val result = runCTest("compot/argument_struct/argument_struct6", listOf(), options())
        assertEquals("x: 4, y: 2, z: 3\nx: 1, y: 2, z: 3\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testArgumentStruct7() {
        val result = runCTest("compot/argument_struct/argument_struct7", listOf(), options())
        assertEquals("x: 4, y: 2, z: 3\nx: 1, y: 2, z: 3\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testArgumentStruct8() {
        val result = runCTest("compot/argument_struct/argument_struct8", listOf(), options())
        assertEquals("x: 1 y: 2 z: 3 w: 4 v: 5\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testArgumentStruct9() {
        val result = runCTest("compot/argument_struct/argument_struct9", listOf(), options())
        assertEquals("x: 1 y: 2 z: 3\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testArgumentStructFloat() {
        val result = runCTest("compot/argument_struct/argument_struct_fp32", listOf(), options())
        assertEquals("x: 1.000000, y: 2.000000\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testArgumentStructFloat1() {
        val result = runCTest("compot/argument_struct/argument_struct1_fp32", listOf(), options())
        assertEquals("x: 1.000000, y: 2.000000, z: 3.000000\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testArgumentStructDouble() {
        val result = runCTest("compot/argument_struct/argument_struct_fp64", listOf(), options())
        assertEquals("x: 1.000000, y: 2.000000\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testArgumentStructDouble1() {
        val result = runCTest("compot/argument_struct/argument_struct1_fp64", listOf(), options())
        assertEquals("x: 1.000000, y: 2.000000, z: 3.000000\n", result.output)
        assertEquals(0, result.exitCode)
    }
}

class ArgumentStructTestO0: ArgumentStructTest() {
    override fun options(): List<String> = listOf()
}

class ArgumentStructTestO1: ArgumentStructTest() {
    override fun options(): List<String> = listOf("-O1")
}