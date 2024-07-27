package shlang

import common.CommonCTest
import kotlin.test.Ignore
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
        val result = runCTest("shlang/algo/cartesian_to_polar", listOf(), options())
        assertEquals("Tests passed\n", result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testHammingDistance() {
        val result = runCTest("shlang/algo/hamming_distance", listOf(), options())
        assertEquals("All tests have successfully passed!\n", result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testHashAdler32() {
        val result = runCTest("shlang/algo/hash_adler32", listOf(), options())
        assertEquals("Tests passed\n", result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testRot13() {
        val result = runCTest("shlang/algo/rot13", listOf(), options())
        assertEquals("All tests have successfully passed!\n", result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testAffineCipher() {
        val result = runCTest("shlang/algo/affine", listOf(), options())
        assertEquals("All tests have successfully passed!\n", result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testNaiveSearch() {
        val result = runCTest("shlang/algo/naive_search", listOf(), options())
        val output = """
            String test: AABCAB12AFAABCABFFEGABCAB
            Test1: search pattern ABCAB
            --Pattern is found at: 1
            --Pattern is found at: 11
            --Pattern is found at: 20
            Test2: search pattern FFF
            Test3: search pattern CAB
            --Pattern is found at: 3
            --Pattern is found at: 13
            --Pattern is found at: 22
            
        """.trimIndent()
        assertEquals(output, result.output)
        assertReturnCode(result, 0)
    }

    @Ignore
    fun testBoyerMooreSearch() {
        val result = runCTest("shlang/algo/boyer_moore_search", listOf(), options())
        val output = """
            String test: AABCAB12AFAABCABFFEGABCAB
            Test1: search pattern ABCAB
            --Pattern is found at: 1
            --Pattern is found at: 11
            --Pattern is found at: 20
            Test2: search pattern FFF
            Test3: search pattern CAB
            --Pattern is found at: 3
            --Pattern is found at: 13
            --Pattern is found at: 22
            
        """.trimIndent()
        assertEquals(output, result.output)
        assertReturnCode(result, 0)
    }

    open fun testRabinKarpSearch() {
        val result = runCTest("shlang/algo/rabin_karp_search", listOf(), options())
        val output = """
            String test: AABCAB12AFAABCABFFEGABCAB
            Test1: search pattern ABCAB
            --Pattern is found at: 1
            --Pattern is found at: 11
            --Pattern is found at: 20
            Test2: search pattern FFF
            Test3: search pattern CAB
            --Pattern is found at: 3
            --Pattern is found at: 13
            --Pattern is found at: 22
            
        """.trimIndent()
        assertEquals(output, result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testIntToString() {
        val result = runCTest("shlang/int_to_string", listOf(), options())
        assertReturnCode(result, 0)
    }

    @Test
    fun testAtoiStrToInteger() {
        val result = runCTest("shlang/c_atoi_str_to_integer", listOf(), options())
        assertReturnCode(result, 0)
    }

    @Test
    fun testBinaryToDecimal() {
        val result = runCTest("shlang/algo/binary_to_decimal", listOf(), options() + "-E")
        assertEquals("All tests have successfully passed!\n", result.output)
        assertReturnCode(result, 0)
    }
}

class AlgoTestsO0: AlgoTests() {
    override fun options(): List<String> = listOf()
}

class AgoTestsO1: AlgoTests() {
    override fun options(): List<String> = listOf("-O1")

    @Ignore
    override fun testRabinKarpSearch() {
        super.testRabinKarpSearch()
    }
}