package opt

import common.CommonIrTest
import kotlin.test.Test
import kotlin.test.assertEquals


abstract class TupleCallTest: CommonIrTest() {
    @Test
    fun testTupleCall() {
        val result = runTest("opt_ir/call/tupleCall", listOf("runtime/runtime.c"), options())
        assertEquals("3\n4\n", result.output)
    }

    @Test
    fun testTupleCall2() {
        val result = runTest("opt_ir/call/tupleCall_i64", listOf("runtime/runtime.c"), options())
        assertEquals("3\n4\n", result.output)
    }

    @Test
    fun testTupleCallf64() {
        val result = runTest("opt_ir/call/tupleCall_f64", listOf("runtime/runtime.c"), options())
        assertEquals("3.000000\n4.000000\n", result.output)
    }

    @Test
    fun testTupleCallf32() {
        val result = runTest("opt_ir/call/tupleCall_f32", listOf("runtime/runtime.c"), options())
        assertEquals("3.000000\n4.000000\n", result.output)
    }

    @Test
    fun testTupleCallf32i32() {
        val result = runTest("opt_ir/call/tupleCall_f32_i32", listOf("runtime/runtime.c"), options())
        assertEquals("3.000000\n4\n", result.output)
    }

    @Test
    fun testTupleCalli32f32() {
        val result = runTest("opt_ir/call/tupleCall_i32_f32", listOf("runtime/runtime.c"), options())
        assertEquals("3\n4.000000\n", result.output)
    }
}

class TupleCallO1Tests: TupleCallTest() {
    override fun options(): List<String> = listOf("-O1")
}

class TupleCallO0Tests: TupleCallTest() {
    override fun options(): List<String> = listOf()
}