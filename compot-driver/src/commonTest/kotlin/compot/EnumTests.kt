package compot

import kotlin.test.*
import common.CommonCTest


abstract class EnumTests: CommonCTest() {
    @Test
    fun test1() {
        val result = runCTest("compot/enum/enum1", listOf(), options())
        assertEquals(0, result.exitCode)
    }

    @Test
    fun test2() {
        val result = runCTest("compot/enum/enum2", listOf(), options())
        assertEquals(0, result.exitCode)
    }

    @Test
    fun test3() {
        val result = runCTest("compot/enum/enum3", listOf(), options())
        assertEquals(0, result.exitCode)
    }

    @Test
    fun test4() {
        val result = runCTest("compot/enum/enum4", listOf(), options())
        assertEquals("Data: (2, 20)\n", result.output)
        assertEquals(0, result.exitCode)
    }
}

class EnumTestsO0: EnumTests() {
    override fun options(): List<String> = listOf()
}

class EnumTestsO1: EnumTests() {
    override fun options(): List<String> = listOf("-O1")
}