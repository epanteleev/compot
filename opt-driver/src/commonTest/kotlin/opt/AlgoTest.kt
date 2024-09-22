package opt

import common.CommonIrTest
import kotlin.test.Test
import kotlin.test.assertEquals


abstract class AlgoTest: CommonIrTest() {
    @Test
    fun testFibOpt() {
        val result = runTest("opt_ir/algo/fib_opt", listOf("runtime/runtime.c"), options())
        assertEquals("21\n", result.output)
    }

    @Test
    fun testFib() {
        val result = runTest("opt_ir/algo/fib", listOf("runtime/runtime.c"), options())
        assertEquals("21\n", result.output)
    }

    @Test
    fun testFibU32() {
        val result = runTest("opt_ir/algo/fib_u32", listOf("runtime/runtime.c"), options())
        assertEquals("21\n", result.output)
    }

    @Test
    fun testFibRecursive() {
        val result = runTest("opt_ir/algo/fib_recursive", listOf("runtime/runtime.c"), options())
        assertEquals("21\n", result.output)
    }

    @Test
    fun testDiscriminant() {
        val result = runTest("opt_ir/algo/discriminant", listOf("runtime/runtime.c"), options())
        assertEquals("-192\n", result.output)
    }

    @Test
    fun testDiscriminantFloat() {
        val result = runTest("opt_ir/algo/discriminant1", listOf("runtime/runtime.c"), options())
        assertEquals("-192.000000\n", result.output)
    }

    @Test
    fun testFactorial() {
        val result = runTest("opt_ir/algo/factorial", listOf("runtime/runtime.c"), options())
        assertEquals("40320\n", result.output)
    }

    @Test
    fun testClamp() {
        val result = runTest("opt_ir/algo/clamp", listOf("runtime/runtime.c"), options())
        assertEquals("9\n10\n8\n", result.output)
    }

    @Test
    fun testClamp1() {
        val result = runTest("opt_ir/algo/clamp1", listOf("runtime/runtime.c"), options())
        assertEquals("9.000000\n10.000000\n8.000000\n", result.output)
    }

    @Test
    fun testRemoveElement() {
        val result = runTest("opt_ir/algo/removeElement", listOf("runtime/runtime.c"), options())
        assertEquals("4 2 0 9 45 55 89 4 23 \n", result.output)
    }

    @Test
    fun testStringReverse() {
        val result = runTest("opt_ir/algo/stringReverse", listOf("runtime/runtime.c"), options())
        assertEquals("dlrow olleH", result.output)
    }

    @Test
    fun testSumLoop() {
        val result = runTest("opt_ir/algo/sumLoop2", listOf("runtime/runtime.c"), options())
        assertEquals("45\n", result.output)
    }

    @Test
    fun testSum() {
        val result = runTest("opt_ir/algo/sum", listOf("runtime/runtime.c"), options())
        assertEquals("16\n", result.output)
    }

    @Test
    fun testSumU32() {
        val result = runTest("opt_ir/algo/sum1", listOf("runtime/runtime.c"), options())
        assertEquals("16.000000\n", result.output)
    }
}

class AlgoO1Tests: AlgoTest() {
    override fun options(): List<String> = listOf("-O1")
}

class AlgoO0Tests: AlgoTest() {
    override fun options(): List<String> = listOf()
}