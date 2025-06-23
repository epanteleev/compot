package compot

import common.CommonCTest
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class ConstantTests: CommonCTest() {
    @Test
    fun test0() {
        val result = runCTest("compot/constant/ulong", listOf(), options())
        assertEquals("hash = 5381\n", result.output)
    }

    @Test
    fun test1() {
        val result = runCTest("compot/constant/bit_invert", listOf(), options())
        assertEquals("inverted = 0\n", result.output)
    }

    @Test
    fun test2() {
        val result = runCTest("compot/constant/bit_invert1", listOf(), options())
        assertEquals("inverted = 0\n", result.output)
    }

    @Test
    fun test3() {
        val result = runCTest("compot/constant/select_str", listOf(), options())
        assertEquals("Success\n", result.output)
    }

    @Test
    fun test4() {
        val result = runCTest("compot/constant/float", listOf(), options())
        assertEquals("3.000000", result.output)
        assertEquals(0, result.exitCode)
    }
}

class ConstantTestsO0: ConstantTests() {
    override fun options(): List<String> = listOf()
}

class ConstantTestsO1: ConstantTests() {
    override fun options(): List<String> = listOf("-O1")
}