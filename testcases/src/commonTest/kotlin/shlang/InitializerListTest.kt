package shlang

import common.CommonCTest
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class InitializerListTest: CommonCTest() {
    @Test
    fun test1() {
        val result = runCTest("shlang/initializerList/initializerList", listOf(), options())
        assertEquals("123\n", result.output)
    }
}

class InitializerListTestsO0: InitializerListTest() {
    override fun options(): List<String> = listOf()
}

class InitializerListTestsO1: InitializerListTest() {
    override fun options(): List<String> = listOf("-O1")
}