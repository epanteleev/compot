package opt

import common.CommonIrTest
import kotlin.test.Test

abstract class SwapTest : CommonIrTest() {
    @Test
    fun testSwap() {
        val result = runTest("opt_ir/swap/swap", listOf("runtime/runtime.c"), options())
        assert(result, "7\n5\n")
    }

    @Test
    fun testSwap1() {
        val result = runTest("opt_ir/swap/swap1", listOf("runtime/runtime.c"), options())
        assert(result, "4 2 0 9 90 45 55 89 23 4 \n")
    }

    @Test
    fun testSwapElements() {
        val result = runTest("opt_ir/swap/swapElements", listOf("runtime/runtime.c"), options())
        assert(result, "4 2 0 9 90 45 55 89 23 4 \n")
    }

    @Test
    fun testSwapStructElements() {
        val result = runTest("opt_ir/swap/swapStructElements", listOf("runtime/runtime.c"), options())
        assert(result, "67 5 \n")
    }
}

class SwapAlgTestO1Tests: SwapTest() {
    override fun options(): List<String> = listOf("-O1")
}

class SwapO0Tests: SwapTest() {
    override fun options(): List<String> = listOf()
}