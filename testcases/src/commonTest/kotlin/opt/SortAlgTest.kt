package opt

import common.CommonIrTest
import kotlin.test.Test

abstract class SortAlgTest : CommonIrTest() {
    @Test
    fun testsBubbleSortFloats() {
        val result = runTest("opt_ir/sort_alg/bubble_sort_fp", listOf("runtime/runtime.c"), options())
        assert(result, "0.000000 2.000000 4.000000 4.000000 9.000000 23.000000 45.000000 55.000000 89.000000 90.000000 \n")
    }

    @Test
    fun testBubbleSort() {
        val result = runTest("opt_ir/sort_alg/bubble_sort", listOf("runtime/runtime.c"), options())
        assert(result, "0 2 4 4 9 23 45 55 89 90 \n")
    }

    @Test
    fun testBubbleSortI8() {
        val result = runTest("opt_ir/sort_alg/bubble_sort_i8", listOf("runtime/runtime.c"), options())
        assert(result, "0 2 4 4 9 23 45 55 89 90 \n")
    }
}

class SortAlgTestO1Tests: SortAlgTest() {
    override fun options(): List<String> = listOf("-O1")
}

class SortAlgTestO0Tests: SortAlgTest() {
    override fun options(): List<String> = listOf()
}