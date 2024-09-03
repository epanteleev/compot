package opt

import common.CommonIrTest
import kotlin.test.Test

abstract class ShiftTest : CommonIrTest() {
    @Test
    fun testShl1() {
        val result = runTest("opt_ir/shift/shl1", listOf("runtime/runtime.c"), options())
        assert(result, "8\n")
    }
}

class ShiftTestO1Tests: ShiftTest() {
    override fun options(): List<String> = listOf("-O1")
}

class ShiftTestO0Tests: ShiftTest() {
    override fun options(): List<String> = listOf()
}