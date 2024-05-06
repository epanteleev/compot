import common.CommonCTest
import common.CommonTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals


class ShlangTests: CommonCTest() {
    @Test
    fun testDoWhile() {
        val result = runCTest("shlang/doWhile", listOf())
        assertReturnCode(result, 20)
    }

    @Test
    fun testOptDoWhile() {
        val result = runOptimizedCTest("shlang/doWhile", listOf())
        assertReturnCode(result, 20)
    }

    @Test
    fun testArrayAccess() {
        val result = runCTest("shlang/arrayAccess", listOf())
        assertReturnCode(result, 3)
    }

    @Test
    fun testAnd1() {
        val result = runCTest("shlang/and1", listOf())
        assertReturnCode(result, 3)
    }

    @Test
    fun testOptArrayAccess() {
        val result = runOptimizedCTest("shlang/arrayAccess", listOf())
        assertReturnCode(result, 3)
    }

    @Test
    fun testArrayAssign() {
        val result = runCTest("shlang/discriminant1", listOf("runtime/runtime.c"))
        assertEquals("1.000000\n", result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testOptArrayAssign() {
        val result = runCTest("shlang/discriminant1", listOf("runtime/runtime.c"))
        assertEquals("1.000000\n", result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testMemsetMemcpy() {
        val result = runCTest("shlang/memset/memset_arrays", listOf("shlang/memset/memset_test.c"))
        assert(result, "Done")
    }

    @Test
    fun testOptMemsetMemcpy() {
        val result = runOptimizedCTest("shlang/memset/memset_arrays", listOf("shlang/memset/memset_test.c"))
        assert(result, "Done")
    }

    @Test
    fun testMemset1() {
        val result = runCTest("shlang/memset1", listOf("runtime/runtime.c"))
        assert(result, "0 0 0 0 0 0 0 0 0 0 \n")
    }

    @Test
    fun testOptMemset1() {
        val result = runOptimizedCTest("shlang/memset1", listOf("runtime/runtime.c"))
        assert(result, "0 0 0 0 0 0 0 0 0 0 \n")
    }

    @Test
    fun testSameNames() {
        val result = runCTest("shlang/sameNames", listOf("runtime/runtime.c"))
        assert(result, "1\n")
    }

    @Test
    fun testHelloWorld() {
        val result = runCTest("shlang/helloWorld", listOf())
        assert(result, "Hello, World!\n")
    }

    @Test
    fun testOptHelloWorld() {
        val result = runOptimizedCTest("shlang/helloWorld", listOf())
        assert(result, "Hello, World!\n")
    }

    @Test
    fun testSum8() {
        val result = runCTest("shlang/sum8", listOf("runtime/runtime.c"))
        assert(result, "8.000000\n")
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testOptSum8() {
        val result = runOptimizedCTest("shlang/sum8", listOf("runtime/runtime.c"))
        assert(result, "8.000000\n")
        assertEquals(0, result.exitCode)
    }

    @Ignore
    fun testInsertionSort() {
        val result = runCTest("shlang/insertionSort", listOf("runtime/runtime.c"))
        assert(result, "11 12 22 25 34 64 \n")
    }

    @Test
    fun testBubbleSort() {
        val result = runCTest("shlang/bubble_sort_int", listOf("runtime/runtime.c"))
        assert(result, "11 12 22 25 34 64 \n")
    }

    @Test
    fun testOptBubbleSort() {
        val result = runOptimizedCTest("shlang/bubble_sort_int", listOf("runtime/runtime.c"))
        assert(result, "11 12 22 25 34 64 \n")
    }

    @Test
    fun testFibonacci() {
        val result = runCTest("shlang/fibonacci1", listOf("runtime/runtime.c"))
        assert(result, "55\n")
    }

    @Test
    fun testOptFibonacci() {
        val result = runOptimizedCTest("shlang/fibonacci1", listOf("runtime/runtime.c"))
        assert(result, "55\n")
    }
}