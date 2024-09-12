package shlang

import common.CommonCTest
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class EnumTests: CommonCTest() {
    @Test
    fun test1() {
        val result = runCTest("shlang/enum/enum1", listOf(), options())
        assertEquals(0, result.exitCode)
    }

    @Test
    fun test2() {
        val result = runCTest("shlang/enum/enum2", listOf(), options())
        assertEquals(0, result.exitCode)
    }
}

class EnumTestsO0: EnumTests() {
    override fun options(): List<String> = listOf()
}

class EnumTestsO1: EnumTests() {
    override fun options(): List<String> = listOf("-O1")
}