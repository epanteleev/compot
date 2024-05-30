package div

import common.CommonIrTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals


abstract class DivideOperationTest: CommonIrTest() {
    @Test
    fun testDivI8() {
        val result = runTest("opt_ir/div/div_i8", listOf("runtime/runtime.c"), options())
        assertEquals("2\n-2\n", result.output)
    }

    @Test
    fun testDivI16() {
        val result = runTest("opt_ir/div/div_i16", listOf("runtime/runtime.c"), options())
        assertEquals("2\n-2\n", result.output)
    }

    @Test
    fun testDivI32() {
        val result = runTest("opt_ir/div/div_i32", listOf("runtime/runtime.c"), options())
        assertEquals("2\n-2\n", result.output)
    }

    @Test
    fun testDivI64() {
        val result = runTest("opt_ir/div/div_i64", listOf("runtime/runtime.c"), options())
        assertEquals("2\n-2\n", result.output)
    }

    @Test
    fun testDivU8() {
        val result = runTest("opt_ir/div/div_u8", listOf("runtime/runtime.c"), options())
        assertEquals("2\n-2\n", result.output)
    }
}

class BaseIrDivideOperationTest: DivideOperationTest() {
    override fun options(): List<String> = listOf()
}

class OptIRDivideOperationTest: DivideOperationTest() {
    override fun options(): List<String> = listOf("-O1")
}