package opt

import common.CommonIrTest
import kotlin.test.Test
import kotlin.test.assertEquals


abstract class MemcpyTests: CommonIrTest() {
    @Test
    fun testMemcpy() {
        val result = runTest("opt_ir/memcpy/memcpy", listOf("runtime/runtime.c"), options())
        assertEquals("Hello world", result.output)
    }

    @Test
    fun testMemcpy1() {
        val result = runTest("opt_ir/memcpy/memcpy1", listOf("runtime/runtime.c"), options())
        assertEquals("4 3 2 1 \n", result.output)
    }

    @Test
    fun testMemcpy2() {
        val result = runTest("opt_ir/memcpy/memcpy2", listOf("runtime/runtime.c"), options())
        assertEquals("4\n3\n", result.output)
    }

    @Test
    fun testMemcpyUnaligned() {
        val result = runTest("opt_ir/memcpy/memcpy_unaligned", listOf("runtime/runtime.c"), options())
        assertEquals("Hello world!", result.output)
    }
}

class MemcpyO1Tests: MemcpyTests() {
    override fun options(): List<String> = listOf("-O1")
}

class MemcpyO0Tests: MemcpyTests() {
    override fun options(): List<String> = listOf()
}