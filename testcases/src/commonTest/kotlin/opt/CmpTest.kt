package opt

import common.CommonIrTest
import kotlin.test.Test


abstract class CmpTest : CommonIrTest() {
    @Test
    fun testFPCompare() {
        val result = runTest("opt_ir/cmp/float_compare", listOf("runtime/runtime.c"), options())
        assert(result, "5.000000\n")
    }

    @Test
    fun testFPCompare2() {
        val result = runTest("opt_ir/cmp/float_compare1", listOf("runtime/runtime.c"), options())
        assert(result, "4.000000\n")
    }
}

class CmpO1Tests: CmpTest() {
    override fun options(): List<String> = listOf("-O1")
}

class CmpO0Tests: CmpTest() {
    override fun options(): List<String> = listOf()
}