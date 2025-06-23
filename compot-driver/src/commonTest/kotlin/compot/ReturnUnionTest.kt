package compot

import common.CommonCTest
import kotlin.test.Test
import kotlin.test.assertEquals

sealed class ReturnUnionTest : CommonCTest() {
    @Test
    fun test1() {
        val result = runCTest("compot/return_union/return_union1", listOf(), options())
        assertEquals("x: 1.000000\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun test2() {
        val result = runCTest("compot/return_union/return_union2", listOf(), options())
        assertEquals("x: 1.000000\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun test3() {
        val result = runCTest("compot/return_union/return_union3", listOf(), options())
        assertEquals("x: 1.000000\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun test4() {
        val result = runCTest("compot/return_union/return_union4", listOf(), options())
        assertEquals("x: 1.000000\n", result.output)
        assertEquals(0, result.exitCode)
    }
}

class ReturnUnionTestO0: ReturnUnionTest() {
    override fun options(): List<String> = listOf()
}

class ReturnUnionTestO1: ReturnUnionTest() {
    override fun options(): List<String> = listOf("-O1")
}