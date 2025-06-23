package compot

import common.CommonCTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals


abstract class DesignationTest: CommonCTest() {
    @Test
    fun test1() {
        val result = runCTest("compot/designation/designation1", listOf(), options())
        assertEquals("x: 1, y: 2\n", result.output)
    }

    @Test
    fun test2() {
        val result = runCTest("compot/designation/designation2", listOf(), options())
        assertEquals("x: 1, y: 2\n", result.output)
    }

    @Test
    fun test3() {
        val result = runCTest("compot/designation/designation3", listOf(), options())
        assertEquals("x: 2, y: 1\n", result.output)
    }

    @Test
    fun test4() {
        val result = runCTest("compot/designation/designation4", listOf(), options())
        assertEquals("arr[0]: 1, arr[1]: 2\n", result.output)
    }

    @Test
    fun test5() {
        val result = runCTest("compot/designation/designation5", listOf(), options())
        assertEquals("arr[0]: 1, arr[1]: 2\n", result.output)
    }

    @Test
    fun test6() {
        val result = runCTest("compot/designation/designation6", listOf(), options())
        assertEquals("arr[0]: 2, arr[1]: 1\n", result.output)
    }

    @Test
    fun test7() {
        val result = runCTest("compot/designation/designation7", listOf(), options())
        assertEquals("arr.data.ptr[0]: 1, arr.data.ptr[1]: 2\n", result.output)
    }

    @Test
    fun test8() {
        val result = runCTest("compot/designation/designation8", listOf(), options())
        assertEquals("arr.data.ptr[0]: 1, arr.data.ptr[1]: 2\n", result.output)
    }

    @Test
    fun test9() {
        val result = runCTest("compot/designation/designation9", listOf(), options())
        assertEquals("x: 0, y: 1, z: 0\n", result.output)
    }
}

class DesignationTestO0: DesignationTest() {
    override fun options(): List<String> = listOf()
}

class DesignationTestO1: DesignationTest() {
    override fun options(): List<String> = listOf("-O1")
}