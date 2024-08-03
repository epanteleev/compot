package shlang

import common.CommonCTest
import kotlin.test.Test
import kotlin.test.assertEquals


abstract class DesignationTest: CommonCTest() {
    @Test
    fun test1() {
        val result = runCTest("shlang/designation/designation1", listOf(), options())
        assertEquals("x: 1, y: 2\n", result.output)
    }

    @Test
    fun test2() {
        val result = runCTest("shlang/designation/designation2", listOf(), options())
        assertEquals("x: 1, y: 2\n", result.output)
    }

    @Test
    fun test3() {
        val result = runCTest("shlang/designation/designation3", listOf(), options())
        assertEquals("x: 2, y: 1\n", result.output)
    }
}

class DesignationTestO0: DesignationTest() {
    override fun options(): List<String> = listOf()
}

class DesignationTestO1: DesignationTest() {
    override fun options(): List<String> = listOf("-O1")
}