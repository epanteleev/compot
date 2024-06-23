package opt

import common.CommonIrTest
import kotlin.test.Test
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

    @Test
    fun testsBubbleSortFloats() {
        val result = runTest("opt_ir/bubble_sort_fp", listOf("runtime/runtime.c"), options())
        assert(result, "0.000000 2.000000 4.000000 4.000000 9.000000 23.000000 45.000000 55.000000 89.000000 90.000000 \n")
    }

    @Test
    fun testMemcpyUnaligned() {
        val result = runTest("opt_ir/memcpy_unaligned", listOf("runtime/runtime.c"), options())
        assertEquals("Hello world!", result.output)
    }

    @Test
    fun testTrueBoolConst() {
        val result = runTest("opt_ir/true_bool_const", listOf("runtime/runtime.c"), options())
        assertEquals("1\n", result.output)
    }

    @Test
    fun testFalseBoolConst() {
        val result = runTest("opt_ir/false_bool_const", listOf("runtime/runtime.c"), options())
        assertEquals("2\n", result.output)
    }

    @Test
    fun testNullcheck() {
        val result = runTest("opt_ir/nullcheck", listOf("runtime/runtime.c"), options())
        assertEquals("10\n", result.output)
    }

    @Test
    fun testIndirectionCall() {
        val result = runTest("opt_ir/indirection_call", listOf("runtime/runtime.c"), options())
        assertEquals("42\n", result.output)
    }

    @Test
    fun testFibOpt() {
        val result = runTest("opt_ir/fib_opt", listOf("runtime/runtime.c"), options())
        assertEquals("21\n", result.output)
    }

    @Test
    fun testCallVarargFun() {
        val result = runTest("opt_ir/call_vararg_fun", listOf(), options())
        assertEquals("HELLO 42", result.output)
    }
}

class OptO1Tests: OptTests() {
    override fun options(): List<String> = listOf("-O1")
}

class OptO0Tests: OptTests() {
    override fun options(): List<String> = listOf()
}