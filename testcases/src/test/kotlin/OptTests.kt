import org.junit.jupiter.api.Test


data class Result(val testName: String, val output: String, val error: String, val exitCode: Int)


class OptTests: CommonTest() {
    @Test
    fun testOpt() {
        val result = runTest("opt_ir/bubble_sort", listOf("runtime/runtime.c"))
        assert(result, "0 2 4 4 9 23 45 55 89 90 \n")
    }
}