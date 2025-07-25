package compot

import common.CommonCTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals


abstract class StructTests: CommonCTest() {
    @Test
    fun testStruct0() {
        val result = runCTest("compot/struct/struct0", listOf("runtime/runtime.c"), options())
        assertEquals("30\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testStruct1() {
        val result = runCTest("compot/struct/struct1", listOf("runtime/runtime.c"), options())
        assertEquals("1\n", result.output)
        assertEquals(1, result.exitCode)
    }

    @Test
    fun testStruct2() {
        val result = runCTest("compot/struct/struct2", listOf("runtime/runtime.c"), options())
        assertEquals("Rect: (10, 20) - (30, 40)\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testStruct3() {
        val result = runCTest("compot/struct/struct3", listOf("runtime/runtime.c"), options())
        assertEquals("Vect3: 10 20 30\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testStruct4() {
        val result = runCTest("compot/struct/struct4", listOf("runtime/runtime.c"), options())
        assertEquals("Vect2: 1 2 4 5\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testStruct5() {
        val result = runCTest("compot/struct/struct5", listOf("runtime/runtime.c"), options())
        assertEquals("Point: (1, 2)\nPoint: (1, 2)\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testStruct6() {
        val result = runCTest("compot/struct/struct6", listOf("runtime/runtime.c"), options())
        assertEquals("Point: (10, 20)\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testStruct7() {
        val result = runCTest("compot/struct/struct7", listOf("runtime/runtime.c"), options())
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
        val result = runCTest("compot/struct/struct8", listOf("runtime/runtime.c"), options())
        assertEquals("Success\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testStruct9() {
        val result = runCTest("compot/struct/struct9", listOf("runtime/runtime.c"), options())
        assertEquals("Data: (2, 20)\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testStruct10() {
        val result = runCTest("compot/struct/struct10", listOf("runtime/runtime.c"), options())
        assertEquals("p1=1, p2=1, p3=1\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testStruct11() {
        val result = runCTest("compot/struct/struct11", listOf("runtime/runtime.c"), options())
        assertEquals(1, result.exitCode)
    }

    @Test
    fun testStruct12() {
        val result = runCTest("compot/struct/struct12", listOf("runtime/runtime.c"), options())
        assertEquals("x: 1, y: 2, z: 3\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testStruct13() {
        val result = runCTest("compot/struct/struct13", listOf(), options())
        assertEquals(10, result.exitCode)
    }

    @Test
    fun testStruct14() {
        val result = runCTest("compot/struct/struct14", listOf(), options())
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testStruct15() {
        val result = runCTest("compot/struct/struct15", listOf(), options())
        assertEquals(10, result.exitCode)
    }

    @Test
    fun testStruct16() {
        val result = runCTest("compot/struct/struct16", listOf(), options())
        assertEquals(10, result.exitCode)
    }

    @Test
    fun testStruct17() {
        val result = runCTest("compot/struct/struct17", listOf(), options())
        assertEquals("a\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testStruct18() {
        val result = runCTest("compot/struct/struct18", listOf(), options())
        assertEquals(1, result.exitCode)
    }

    @Test
    fun testStruct19() {
        val result = runCTest("compot/struct/struct19", listOf(), options())
        assertEquals(99, result.exitCode)
    }

    @Test
    fun testStruct20() {
        val result = runCTest("compot/struct/struct20", listOf(), options())
        assertEquals(15, result.exitCode)
    }

    @Test
    fun testStruct21() {
        val result = runCTest("compot/struct/struct20", listOf(), options())
        assertEquals(15, result.exitCode)
    }

    @Test
    @Ignore
    fun testStruct22() {
        val result = runCTest("compot/struct/struct22", listOf(), options())
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testStruct23() {
        val result = runCTest("compot/struct/struct23", listOf(), options())
        assertEquals("h1: 1 2 3 4 5 6 7 8 9\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testStruct24() {
        val result = runCTest("compot/struct/struct24", listOf(), options())
        assertEquals("h1: 1 2 3 4 5 6 7 8 9\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testStruct25() {
        val result = runCTest("compot/struct/struct25", listOf(), options())
        assertEquals(6, result.exitCode)
    }
}

class StructTestsO0: StructTests() {
    override fun options(): List<String> = listOf()
}

class StructTestsO1: StructTests() {
    override fun options(): List<String> = listOf("-O1")
}

class StructTestO1fPIC: StructTests() {
    override fun options(): List<String> = listOf("-O1", "-fPIC")
}