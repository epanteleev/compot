package shlang

import common.CommonCTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals


abstract class StructTests: CommonCTest() {
    @Test
    fun testStruct0() {
        val result = runCTest("shlang/struct/struct0", listOf("runtime/runtime.c"), options())
        assertEquals("30\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testStruct1() {
        val result = runCTest("shlang/struct/struct1", listOf("runtime/runtime.c"), options())
        assertEquals("0\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testStruct2() {
        val result = runCTest("shlang/struct/struct2", listOf("runtime/runtime.c"), options())
        assertEquals("Rect: (10, 20) - (30, 40)\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testStruct3() {
        val result = runCTest("shlang/struct/struct3", listOf("runtime/runtime.c"), options())
        assertEquals("Vect3: 10 20 30\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testStruct4() {
        val result = runCTest("shlang/struct/struct4", listOf("runtime/runtime.c"), options())
        assertEquals("Vect2: 1 2 4 5\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testStruct5() {
        val result = runCTest("shlang/struct/struct5", listOf("runtime/runtime.c"), options())
        assertEquals("Point: (1, 2)\nPoint: (1, 2)\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testStruct6() {
        val result = runCTest("shlang/struct/struct6", listOf("runtime/runtime.c"), options())
        assertEquals("Point: (10, 20)\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testStruct7() {
        val result = runCTest("shlang/struct/struct7", listOf("runtime/runtime.c"), options())
        val expected = """
            |0 -1
            |1 0
            |2 1
            |3 2
            |4 3
            |5 4
            |""".trimMargin()
        assertEquals(expected, result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testStruct8() {
        val result = runCTest("shlang/struct/struct8", listOf("runtime/runtime.c"), options())
        assertEquals("Success\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testStruct9() {
        val result = runCTest("shlang/struct/struct9", listOf("runtime/runtime.c"), options())
        assertEquals("Data: (2, 20)\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testStruct10() {
        val result = runCTest("shlang/struct/struct10", listOf("runtime/runtime.c"), options())
        assertEquals("p1=1, p2=1, p3=1\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testStruct11() {
        val result = runCTest("shlang/struct/struct11", listOf("runtime/runtime.c"), options())
        assertEquals(1, result.exitCode)
    }

    @Test
    fun testStruct12() {
        val result = runCTest("shlang/struct/struct12", listOf("runtime/runtime.c"), options())
        assertEquals("x: 1, y: 2, z: 3\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testReturnStruct() {
        val result = runCTest("shlang/struct/return_struct", listOf("runtime/runtime.c"), options())
        assertEquals("x: 1, y: 2\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testReturnStruct1() {
        val result = runCTest("shlang/struct/return_struct1", listOf("runtime/runtime.c"), options())
        assertEquals("x: 1, y: 2, z: 3\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testReturnStruct2() {
        val result = runCTest("shlang/struct/return_struct2", listOf("runtime/runtime.c"), options())
        assertEquals("x: 1.000000, y: 2.000000, z: 3.000000\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testReturnStruct3() {
        val result = runCTest("shlang/struct/return_struct3", listOf("runtime/runtime.c"), options())
        assertEquals("x: 1.000000, y: 2.000000, z: 3\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testReturnStruct4() {
        val result = runCTest("shlang/struct/return_struct4", listOf("runtime/runtime.c"), options())
        assertEquals("x: 1 y: 2 z: 3 w: 4 v: 5 u: 6 t: 7 s: 8\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testReturnStruct5() {
        val result = runCTest("shlang/struct/return_struct5", listOf("runtime/runtime.c"), options())
        assertEquals("x: 1, y: 2, z: 3\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testArgumentStruct() {
        val result = runCTest("shlang/struct/argument_struct", listOf("runtime/runtime.c"), options())
        assertEquals("x: 1, y: 2\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testArgumentStruct1() {
        val result = runCTest("shlang/struct/argument_struct1", listOf(), options())
        assertEquals("x: 1, y: 2, z: 3\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testArgumentStruct2() {
        val result = runCTest("shlang/struct/argument_struct2", listOf(), options())
        assertEquals("x: 1, y: 2, z: 3\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testArgumentStruct3() {
        val result = runCTest("shlang/struct/argument_struct3", listOf(), options())
        assertEquals("x: 1 y: 2 z: 3 w: 4 v: 5 u: 6 t: 7 s: 8\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testArgumentStruct4() {
        val result = runCTest("shlang/struct/argument_struct4", listOf(), options())
        assertEquals("x: 1, y: 2, z: 3\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testArgumentStruct5() {
        val result = runCTest("shlang/struct/argument_struct5", listOf(), options())
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testArgumentStruct6() {
        val result = runCTest("shlang/struct/argument_struct6", listOf(), options())
        assertEquals("x: 4, y: 2, z: 3\nx: 1, y: 2, z: 3\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testArgumentStructFloat() {
        val result = runCTest("shlang/struct/argument_struct_fp32", listOf(), options())
        assertEquals("x: 1.000000, y: 2.000000\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testArgumentStructFloat1() {
        val result = runCTest("shlang/struct/argument_struct1_fp32", listOf(), options())
        assertEquals("x: 1.000000, y: 2.000000, z: 3.000000\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testArgumentStructDouble() {
        val result = runCTest("shlang/struct/argument_struct_fp64", listOf(), options())
        assertEquals("x: 1.000000, y: 2.000000\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testArgumentStructDouble1() {
        val result = runCTest("shlang/struct/argument_struct1_fp64", listOf(), options())
        assertEquals("x: 1.000000, y: 2.000000, z: 3.000000\n", result.output)
        assertEquals(0, result.exitCode)
    }
}

class StructTestsO0: StructTests() {
    override fun options(): List<String> = listOf()
}

class StructTestsO1: StructTests() {
    override fun options(): List<String> = listOf("-O1")
}