import kotlin.test.Test


class ShlangTests: CommonTest() {
    @Test
    fun testDoWhile() {
        val result = runCTest("shlang/doWhile", listOf())
        assertReturnCode(result, 20)
    }

    @Test
    fun testArrayAccess() {
        val result = runCTest("shlang/arrayAccess", listOf())
        assertReturnCode(result, 3)
    }

    @Test
    fun testBubbleSort() {
        val result = runCTest("shlang/bubble_sort", listOf("runtime/runtime.c"))
        assert(result, "11 12 22 25 34 64 \n")
    }

    @Test
    fun testFibonacci() {
        val result = runCTest("shlang/fibonacci1", listOf("runtime/runtime.c"))
        assert(result, "55\n")
    }
}