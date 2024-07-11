package shlang

import common.CommonCTest
import kotlin.test.Test
import kotlin.test.assertEquals


abstract class AlgoTests: CommonCTest() {
    @Test
    fun testHashCRC32() {
        val result = runCTest("shlang/algo/hash_crc32", listOf(), options())
        assertEquals("Tests passed\n", result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testHashXor8() {
        val result = runCTest("shlang/algo/hash_xor8", listOf(), options())
        assertEquals("Tests passed\n", result.output)
        assertReturnCode(result, 0)
    }
}

class AlgoTestsO0: AlgoTests() {
    override fun options(): List<String> = listOf()
}

class AgoTestsO1: AlgoTests() {
    override fun options(): List<String> = listOf("-O1")
}