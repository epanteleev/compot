package compot

import common.CommonCTest
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class CharTest: CommonCTest() {
    @Test
    fun test1() {
        val result = runCTest("compot/char/char1", listOf(), options())
        assertEquals(0, result.exitCode)
    }

    @Test
    fun test2() {
        val result = runCTest("compot/char/char2", listOf(), options())
        assertEquals("\n", result.output)
        assertEquals(0, result.exitCode)
    }
}

class CharTestsO0: CharTest() {
    override fun options(): List<String> = listOf()
}

class CharTestsO1: CharTest() {
    override fun options(): List<String> = listOf("-O1")
}