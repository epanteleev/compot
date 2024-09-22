package opt

import common.CommonIrTest
import kotlin.test.Test
import kotlin.test.assertEquals


abstract class SwitchTest: CommonIrTest() {
    @Test
    fun testSwitch() {
        val result = runTest("opt_ir/switch/switch", listOf("runtime/runtime.c"), options())
        assertEquals("20\n30\n10\n40\n", result.output)
    }

    @Test
    fun testSwitch1() {
        val result = runTest("opt_ir/switch/switch1", listOf("runtime/runtime.c"), options())
        assertEquals("40\n40\n10\n40\n", result.output)
    }
}

class SwitchO1Tests: SwitchTest() {
    override fun options(): List<String> = listOf("-O1")
}

class SwitchO0Tests: SwitchTest() {
    override fun options(): List<String> = listOf()
}