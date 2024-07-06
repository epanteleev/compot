package shlang

import common.CommonCTest
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class InitializerListTest: CommonCTest() {
    @Test
    fun test1() {
        val result = runCTest("shlang/initializerList/initializerList", listOf(), options())
        assertEquals("123", result.output)
    }

    @Test
    fun test2() {
        val result = runCTest("shlang/initializerList/initializerList1", listOf(), options())
        assertEquals("point.x = 1, point.y = 2.000", result.output)
    }
}

class InitializerListTestsO0: InitializerListTest() {
    override fun options(): List<String> = listOf()
}

class InitializerListTestsO1: InitializerListTest() {
    override fun options(): List<String> = listOf("-O1")
}