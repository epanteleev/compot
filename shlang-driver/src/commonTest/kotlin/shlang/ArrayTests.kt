package shlang

import common.CommonCTest
import kotlin.test.Test
import kotlin.test.assertEquals


abstract class ArrayTests: CommonCTest() {
    @Test
    fun test1() {
        val result = runCTest("shlang/array/array1", listOf(), options())
        assertEquals("Array: (10, 20)\n", result.output)
    }
}

class ArrayTestsO0: ArrayTests() {
    override fun options(): List<String> = listOf()
}

class ArrayTestsO1: ArrayTests() {
    override fun options(): List<String> = listOf("-O1")
}