package cvt

import CommonTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals


class IRConversionsTest: CommonTest() {

    @Test
    fun testI32toF32() {
        val result = runTest("opt_ir/cvt/i32_to_f32", listOf("runtime/runtime.c"))
        assertEquals("-1.000000\n", result.output)
    }

    @Test
    fun testOptI32toF32() {
        val result = runOptimizedTest("opt_ir/cvt/i32_to_f32", listOf("runtime/runtime.c"))
        assertEquals("-1.000000\n", result.output)
    }

    @Test
    fun testI8toF32() {
        val result = runTest("opt_ir/cvt/i8_to_f32", listOf("runtime/runtime.c"))
        assertEquals("-1.000000\n", result.output)
    }

    @Test
    fun testOptI8toF32() {
        val result = runOptimizedTest("opt_ir/cvt/i8_to_f32", listOf("runtime/runtime.c"))
        assertEquals("-1.000000\n", result.output)
    }

    @Test
    fun testU8toF32() {
        val result = runTest("opt_ir/cvt/u8_to_f32", listOf("runtime/runtime.c"))
        assertEquals("255.000000\n", result.output)
    }

    @Test
    fun testOptu8toF32() {
        val result = runOptimizedTest("opt_ir/cvt/u8_to_f32", listOf("runtime/runtime.c"))
        assertEquals("255.000000\n", result.output)
    }

    @Test
    fun testI8toF64() {
        val result = runTest("opt_ir/cvt/i8_to_f64", listOf("runtime/runtime.c"))
        assertEquals("-1.000000\n", result.output)
    }

    @Test
    fun testOptI8toF64() {
        val result = runOptimizedTest("opt_ir/cvt/i8_to_f64", listOf("runtime/runtime.c"))
        assertEquals("-1.000000\n", result.output)
    }
}