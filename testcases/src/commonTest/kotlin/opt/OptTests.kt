package opt

import common.CommonIrTest
import kotlin.test.Test
import kotlin.test.assertEquals


abstract class OptTests: CommonIrTest() {
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
    fun testCallVarargFun() {
        val result = runTest("opt_ir/call_vararg_fun", listOf(), options())
        assertEquals("HELLO 42", result.output)
    }

    @Test
    fun testStructAccess() {
        val result = runTest("opt_ir/struct_access", listOf("runtime/runtime.c"), options())
        assertEquals("14\n", result.output)
    }

    @Test
    fun testStructAccess1() {
        val result = runTest("opt_ir/struct_access1", listOf("runtime/runtime.c"), options())
        assertEquals("16\n", result.output)
    }

    @Test
    fun testManyArguments() {
        val result = runTest("opt_ir/manyArguments", listOf("runtime/runtime.c"), options())
        assertEquals("36\n", result.output)
    }

    @Test
    fun testManyArguments1() {
        val result = runTest("opt_ir/manyArguments1", listOf("runtime/runtime.c"), options())
        assertEquals("36.000000\n", result.output)
    }

    @Test
    fun testSelect() {
        val result = runTest("opt_ir/select", listOf("runtime/runtime.c"), options())
        assertEquals("0\n1\n", result.output)
    }

    @Test
    fun testManyBranched() {
        val result = runTest("opt_ir/manyBranches", listOf("runtime/runtime.c"), options())
        assertEquals("7\n0\n", result.output)
    }

    @Test
    fun testGetAddress() {
        val result = runTest("opt_ir/getAddress", listOf("runtime/runtime.c"), options())
        assertEquals("90\n", result.output)
    }

    @Test
    fun testGetAddress1() {
        val result = runTest("opt_ir/getAddress1", listOf("runtime/runtime.c"), options())
        assertEquals("90\n", result.output)
    }

    @Test
    fun testHelloWorld() {
        val result = runTest("opt_ir/hello_world", listOf("runtime/runtime.c"), options())
        assertEquals("Hello world", result.output)
    }

    @Test
    fun testHelloWorld1() {
        val result = runTest("opt_ir/hello_world1", listOf("runtime/runtime.c"), options())
        assertEquals("Hello world", result.output)
    }
}

class OptO1Tests: OptTests() {
    override fun options(): List<String> = listOf("-O1")
}

class OptO0Tests: OptTests() {
    override fun options(): List<String> = listOf()
}