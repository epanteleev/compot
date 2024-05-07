import common.CommonIrTest
import common.CommonTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals


abstract class OptTests: CommonIrTest() {
    @Test
    fun testBubbleSort() {
        val result = runTest("opt_ir/bubble_sort", listOf("runtime/runtime.c"), options())
        assert(result, "0 2 4 4 9 23 45 55 89 90 \n")
    }

    @Test
    fun testLess() {
        val result = runTest("opt_ir/less", listOf("runtime/runtime.c"), options())
        assertEquals("0\n", result.output)
    }

    @Test
    fun testLessFP32() {
        val result = runTest("opt_ir/less_fp32", listOf("runtime/runtime.c"), options())
        assertEquals("0\n", result.output)
    }

    @Test
    fun testMemcpy() {
        val result = runTest("opt_ir/memcpy", listOf("runtime/runtime.c"), options())
        assertEquals("Hello world", result.output)
    }
}

class OptO1Tests: OptTests() {
    override fun options(): List<String> = listOf("-O1")
}

class OptO0Tests: OptTests() {
    override fun options(): List<String> = listOf()
}