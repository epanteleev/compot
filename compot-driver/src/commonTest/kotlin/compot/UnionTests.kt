package compot

import kotlin.test.*
import common.CommonCTest


sealed class UnionTests : CommonCTest() {
    @Test
    fun testUnion0() {
        val result = runCTest("compot/union/union0", listOf("runtime/runtime.c"), options())
        assertEquals("1.000000\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testUnion1() {
        val result = runCTest("compot/union/union1", listOf("runtime/runtime.c"), options())
        assertEquals(1, result.exitCode)
    }

    @Test
    fun testUnion2() {
        val result = runCTest("compot/union/union2", listOf("runtime/runtime.c"), options())
        assertEquals("1.000000\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testUnion3() {
        val result = runCTest("compot/union/union3", listOf("runtime/runtime.c"), options())
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testUnion4() {
        val result = runCTest("compot/union/union4", listOf("runtime/runtime.c"), options())
        assertEquals(1, result.exitCode)
    }

    @Test
    fun testUnion5() {
        val result = runCTest("compot/union/union5", listOf("runtime/runtime.c"), options())
        assertEquals("4607182418800017408\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testUnion6() {
        val result = runCTest("compot/union/union6", listOf("runtime/runtime.c"), options())
        assertEquals("u.key_tt: a, u.next: 2, u.key_val: 0.300000, i_val: 0.000000\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testUnion7() {
        val result = runCTest("compot/union/union7", listOf("runtime/runtime.c"), options())
        assertEquals("545460846593\n", result.output)
        assertEquals(13, result.exitCode)
    }
}

class UnionTestsO0: UnionTests() {
    override fun options(): List<String> = listOf()
}

class UnionTestsO1: UnionTests() {
    override fun options(): List<String> = listOf("-O1")
}