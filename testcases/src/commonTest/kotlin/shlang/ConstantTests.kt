package shlang

import common.CommonCTest
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class ConstantTests: CommonCTest() {
    @Test
    fun test0() {
        val result = runCTest("shlang/constant/ulong", listOf(), options())
        assertEquals("hash = 5381\n", result.output)
    }
}

class ConstantTestsO0: ConstantTests() {
    override fun options(): List<String> = listOf()
}

class ConstantTestsO1: ConstantTests() {
    override fun options(): List<String> = listOf("-O1")
}