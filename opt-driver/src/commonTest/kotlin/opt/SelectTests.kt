package opt

import common.CommonIrTest
import kotlin.test.Test
import kotlin.test.assertEquals


abstract class SelectTests : CommonIrTest() {
    @Test
    fun testI32() {
        val result = runTest("opt_ir/select/select_i32", listOf("runtime/runtime.c"), options())
        assertEquals("0\n1\n", result.output)
    }

    @Test
    fun testI8() {
        val result = runTest("opt_ir/select/select_i8", listOf("runtime/runtime.c"), options())
        assertEquals("0\n1\n", result.output)
    }

    @Test
    fun testU8() {
        val result = runTest("opt_ir/select/select_u8", listOf("runtime/runtime.c"), options())
        assertEquals("2\n1\n", result.output)
    }
}

class SelectO1Tests: SelectTests() {
    override fun options(): List<String> = listOf("-O1")
}

class SelectO0Tests: SelectTests() {
    override fun options(): List<String> = listOf()
}