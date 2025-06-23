package compot

import common.CommonCTest
import kotlin.test.Test
import kotlin.test.assertEquals


abstract class UbCompotTests: CommonCTest() {
    @Test
    fun test1() {
        val result = runCTest("compot/ub/forLoop3", listOf(), options())
        assertEquals("Success", result.output)
    }
}

class UbCompotTestsO0: UbCompotTests() {
    override fun options(): List<String> = listOf()
}

class UbCompotTestsO1: UbCompotTests() {
    override fun options(): List<String> = listOf("-O1")
}