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

    @Test
    fun testHashSDBM() {
        val result = runCTest("shlang/algo/hash_sdbm", listOf(), options())
        assertEquals("Tests passed\n", result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testMatrixMultiplication() {
        val result = runCTest("shlang/algo/matrix_multiplication", listOf(), options())
        assertEquals("Tests passed\n", result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testBinarySearch() {
        val result = runCTest("shlang/algo/binary_search", listOf(), options())
        assertEquals("Test 1.... passed recursive... passed iterative...\n" +
                "Test 2.... passed recursive... passed iterative...\n", result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testExponentialSearch() {
        val result = runCTest("shlang/algo/exponential_search", listOf(), options())
        assertEquals("All tests have successfully passed!\n", result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testCartesianToPolar() {
        val result = runCTest("shlang/algo/cartesian_to_polar", listOf(), options() + "-E")
        assertEquals("Tests passed\n", result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testHammingDistance() {
        val result = runCTest("shlang/algo/hamming_distance", listOf(), options())
        assertEquals("All tests have successfully passed!\n", result.output)
        assertReturnCode(result, 0)
    }
}

class AlgoTestsO0: AlgoTests() {
    override fun options(): List<String> = listOf()
}

class AgoTestsO1: AlgoTests() {
    override fun options(): List<String> = listOf("-O1")
}