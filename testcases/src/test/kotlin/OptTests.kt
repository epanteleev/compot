import org.junit.jupiter.api.Test
import kotlin.test.assertEquals


data class Result(val testName: String, val output: String, val error: String, val exitCode: Int)


class OptTests: CommonTest() {
    @Test
    fun testBubbleSort() {
        val result = runTest("opt_ir/bubble_sort", listOf("runtime/runtime.c"))
        assert(result, "0 2 4 4 9 23 45 55 89 90 \n")
    }

    @Test
    fun testOptBubbleSort() {
        val result = runOptimizedTest("opt_ir/bubble_sort", listOf("runtime/runtime.c"))
        assert(result, "0 2 4 4 9 23 45 55 89 90 \n")
    }

    @Test
    fun testLess() {
        val result = runTest("opt_ir/less", listOf("runtime/runtime.c"))
        assertEquals("0\n", result.output)
    }

    @Test
    fun testOptLess() {
        val result = runOptimizedTest("opt_ir/less", listOf("runtime/runtime.c"))
        assertEquals("0\n", result.output)
    }
}