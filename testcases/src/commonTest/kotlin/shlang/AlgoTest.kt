package shlang

import common.CommonCTest
import kotlin.test.Test


abstract class AlgoTests: CommonCTest() {
    @Test
    fun test1() {
        val result = runCTest("shlang/algo/hash_crc32", listOf(), options())
        assertReturnCode(result, 0)
    }
}

class AlgoTestsO0: AlgoTests() {
    override fun options(): List<String> = listOf()
}

class AgoTestsO1: AlgoTests() {
    override fun options(): List<String> = listOf("-O1")
}