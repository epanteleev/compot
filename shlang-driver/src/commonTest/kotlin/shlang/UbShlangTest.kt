package shlang

import common.CommonCTest
import kotlin.test.Test
import kotlin.test.assertEquals


abstract class UbShlangTests: CommonCTest() {
    @Test
    fun test1() {
        val result = runCTest("shlang/ub/forLoop3", listOf(), options())
        assertEquals("Success", result.output)
    }
}

class UbShlangTestsO0: UbShlangTests() {
    override fun options(): List<String> = listOf()
}

class UbShlangTestsO1: UbShlangTests() {
    override fun options(): List<String> = listOf("-O1")
}