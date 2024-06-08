package cvt

import common.CommonIrTest
import kotlin.test.Test
import kotlin.test.assertEquals


abstract class IRConversionsTest: CommonIrTest() {
    @Test
    fun testI32toF32() {
        val result = runTest("opt_ir/cvt/i32_to_f32", listOf("runtime/runtime.c"), options())
        assertEquals("-1.000000\n", result.output)
    }

    @Test
    fun testI8toF32() {
        val result = runTest("opt_ir/cvt/i8_to_f32", listOf("runtime/runtime.c"), options())
        assertEquals("-1.000000\n", result.output)
    }

    @Test
    fun testU8toF32() {
        val result = runTest("opt_ir/cvt/u8_to_f32", listOf("runtime/runtime.c"), options())
        assertEquals("255.000000\n", result.output)
    }

    @Test
    fun testI8toF64() {
        val result = runTest("opt_ir/cvt/i8_to_f64", listOf("runtime/runtime.c"), options())
        assertEquals("-1.000000\n", result.output)
    }

    @Test
    fun testU32toF32() {
        val result = runTest("opt_ir/cvt/u32_to_f32", listOf("runtime/runtime.c"), options())
        assertEquals("255.000000\n", result.output)
    }

    @Test
    fun testF32toI8() {
        val result = runTest("opt_ir/cvt/f32_to_i8", listOf("runtime/runtime.c"), options())
        assertEquals("-1\n", result.output)
    }

    @Test
    fun testF32toU8() {
        val result = runTest("opt_ir/cvt/f32_to_u8", listOf("runtime/runtime.c"), options())
        assertEquals("255\n", result.output)
    }

    @Test
    fun testI64toPtr() {
        val result = runTest("opt_ir/cvt/i64_to_ptr", listOf("runtime/runtime.c"), options())
        assertEquals("7\n", result.output)
    }
}

class BaseIrConversionTest: IRConversionsTest() {
    override fun options(): List<String> = listOf()
}

class OptIRConversionTest: IRConversionsTest() {
    override fun options(): List<String> = listOf("-O1")
}