import common.CommonIrTest
import kotlin.test.Test
import kotlin.test.assertEquals


abstract class UBTests: CommonIrTest() {
    @Test
    fun testUninitializedVar() {
        val result = runTest("opt_ir/ub/uninitialized_var", listOf("runtime/runtime.c"), options())
        assertEquals("32\n", result.output)
    }
}

class UBO1Tests: UBTests() {
    override fun options(): List<String> = listOf("-O1")
}

class UBO0Tests: UBTests() {
    override fun options(): List<String> = listOf()
}