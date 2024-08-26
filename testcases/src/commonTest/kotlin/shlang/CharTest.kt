package shlang

import common.CommonCTest
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class CharTest: CommonCTest() {
    @Test
    fun test0() {
        val result = runCTest("shlang/char/char1", listOf(), options())
        assertEquals(0, result.exitCode)
    }
}

class CharTestsO0: CharTest() {
    override fun options(): List<String> = listOf()
}

class CharTestsO1: CharTest() {
    override fun options(): List<String> = listOf("-O1")
}