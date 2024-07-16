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
}

class TupleCallO1Tests: TupleCallTest() {
    override fun options(): List<String> = listOf("-O1")
}

class TupleCallO0Tests: TupleCallTest() {
    override fun options(): List<String> = listOf()
}