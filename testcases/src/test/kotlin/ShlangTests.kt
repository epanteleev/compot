import common.CommonCTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals


abstract class ShlangTests: CommonCTest() {
    @Test
    fun testDoWhile() {
        val result = runCTest("shlang/doWhile", listOf(), options())
        assertReturnCode(result, 20)
    }

    @Test
    fun testArrayAccess() {
        val result = runCTest("shlang/arrayAccess", listOf(), options())
        assertReturnCode(result, 3)
    }

    @Test
    fun testAnd1() {
        val result = runCTest("shlang/and1", listOf(), options())
        assertReturnCode(result, 3)
    }

    @Test
    fun testDiscriminant() {
        val result = runCTest("shlang/discriminant1", listOf("runtime/runtime.c"), options())
        assertEquals("1.000000\n", result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testMemsetMemcpy() {
        val result = runCTest("shlang/memset/memset_arrays", listOf("shlang/memset/memset_test.c"), options())
        assert(result, "Done")
    }

    @Test
    fun testMemset1() {
        val result = runCTest("shlang/memset1", listOf("runtime/runtime.c"), options())
        assert(result, "0 0 0 0 0 0 0 0 0 0 \n")
    }

    @Test
    fun testSameNames() {
        val result = runCTest("shlang/sameNames", listOf("runtime/runtime.c"), options())
        assert(result, "1\n")
    }

    @Test
    fun testHelloWorld() {
        val result = runCTest("shlang/helloWorld", listOf(), options())
        assert(result, "Hello, World!\n")
    }

    @Test
    fun testSum8() {
        val result = runCTest("shlang/sum8", listOf("runtime/runtime.c"), options())
        assert(result, "8.000000\n")
        assertEquals(0, result.exitCode)
    }

    @Ignore
    fun testInsertionSort() {
        val result = runCTest("shlang/insertionSort", listOf("runtime/runtime.c"), options())
        assert(result, "11 12 22 25 34 64 \n")
    }

    @Test
    fun testBubbleSort() {
        val result = runCTest("shlang/bubble_sort_int", listOf("runtime/runtime.c"), options())
        assert(result, "11 12 22 25 34 64 \n")
    }

    @Test
    fun testFibonacci() {
        val result = runCTest("shlang/fibonacci1", listOf("runtime/runtime.c"), options())
        assert(result, "55\n")
    }

    @Test
    fun testForLoop0() {
        val result = runCTest("shlang/forLoop0", listOf("runtime/runtime.c"), options())
        assert(result, "Done")
    }

    @Test
    fun testForLoop1() {
        val result = runCTest("shlang/forLoop1", listOf("runtime/runtime.c"), options())
        assert(result, "Done")
    }
}

class ShlangTestsO0: ShlangTests() {
    override fun options(): List<String> = listOf()
}

class ShlangTestsO1: ShlangTests() {
    override fun options(): List<String> = listOf("-O1")
}