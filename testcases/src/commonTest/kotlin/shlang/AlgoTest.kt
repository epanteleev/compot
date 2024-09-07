package shlang

import common.CommonCTest
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
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
    fun testHashDjb2() {
        val result = runCTest("shlang/algo/hash_djb2", listOf(), options())
        assertEquals("Tests passed\n", result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testHashBlake2() {
        val result = runCTest("shlang/algo/hash_blake2b", listOf(), options())
        assertEquals("All tests have successfully passed!\n", result.output)
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
        val output = """
            Test 0.... (0.66, 1.1).... passed
            Test 1.... (0.058, -3.2).... passed
            Test 2.... (3.2, -3.2).... passed
            Test 3.... (0.85, -0.78).... passed
            Test 4.... (-4.7, -1.8).... passed
            
        """.trimIndent()

        assertEquals(output, result.output)
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

    @Test
    fun testRabinKarpSearch() {
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
        assertEquals("<<<< TEST FUNCTION >>>>\n<<<< TEST DONE >>>>\n", result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testBinaryToDecimal() {
        val result = runCTest("shlang/algo/binary_to_decimal", listOf(), options())
        assertEquals("All tests have successfully passed!\n", result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testSudokuSolver() {
        val result = runCTest("shlang/algo/sudoku_solver", listOf(), options())
        val expected = FileSystem.SYSTEM.read("$TESTCASES_DIR/expected_out/shlang/algo/sudoku_solver.output".toPath()) { //TODO
            readUtf8()
        }
        assertEquals(expected, result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testSegmentTree() {
        val result = runCTest("shlang/algo/segment_tree", listOf(), options())
        assertEquals("All tests passed\n", result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testAlaw() {
        val result = runCTest("shlang/algo/alaw", listOf(), options())
        assertEquals("All tests passed!\n", result.output)
        assertReturnCode(result, 0)
    }
}

class AlgoTestsO0: AlgoTests() {
    override fun options(): List<String> = listOf()
}

class AgoTestsO1: AlgoTests() {
    override fun options(): List<String> = listOf("-O1")
}