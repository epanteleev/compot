package opt

import common.CommonIrTest
import kotlin.test.Test

abstract class FillIntArrayTest : CommonIrTest() {
    @Test
    fun testsFillInArray() {
        val result = runTest("opt_ir/fill_in_array/fill_in_array0", listOf("runtime/runtime.c"), options())
        assert(result, "01234\n")
    }

    @Test
    fun testsFillInArray1() {
        val result = runTest("opt_ir/fill_in_array/fill_in_array1", listOf("runtime/runtime.c"), options())
        assert(result, "01234\n")
    }

    @Test
    fun testsFillInArray2() {
        val result = runTest("opt_ir/fill_in_array/fill_in_array2", listOf("runtime/runtime.c"), options())
        assert(result, "01234\n")
    }

    @Test
    fun testsFillInArray3() {
        val result = runTest("opt_ir/fill_in_array/fill_in_array3", listOf("runtime/runtime.c"), options())
        assert(result, "0123456789\n")
    }

    @Test
    fun testsFillInArray4() {
        val result = runTest("opt_ir/fill_in_array/fill_in_array4", listOf("runtime/runtime.c"), options())
        assert(result, "01234\n")
    }

    @Test
    fun testsFillInArray5() {
        val result = runTest("opt_ir/fill_in_array/fill_in_array5", listOf("runtime/runtime.c"), options())
        assert(result, "01234\n")
    }

    @Test
    fun testsFillInFPArray1() {
        val result = runTest("opt_ir/fill_in_array/fill_in_fp_array1", listOf("runtime/runtime.c"), options())
        assert(result, "0.000000 1.000000 2.000000 3.000000 4.000000 \n")
    }

    @Test
    fun testsFillInFPArray2() {
        val result = runTest("opt_ir/fill_in_array/fill_in_fp_array2", listOf("runtime/runtime.c"), options())
        assert(result, "0.000000 1.000000 2.000000 3.000000 4.000000 \n")
    }
}

class FillIntArrayO1Tests: FillIntArrayTest() {
    override fun options(): List<String> = listOf("-O1")
}

class FillIntArrayO0Tests: FillIntArrayTest() {
    override fun options(): List<String> = listOf()
}