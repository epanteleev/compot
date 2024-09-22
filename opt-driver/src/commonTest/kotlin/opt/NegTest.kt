package opt

import common.CommonIrTest
import kotlin.test.Test

abstract class NegTest : CommonIrTest() {
    @Test
    fun testNeg() {
        val result = runTest("opt_ir/neg/neg", listOf("runtime/runtime.c"), options())
        assert(result, "1\n")
    }

    @Test
    fun testNeg1() {
        val result = runTest("opt_ir/neg/neg1", listOf("runtime/runtime.c"), options())
        assert(result, "1.000000\n")
    }

    @Test
    fun testNeg2() {
        val result = runTest("opt_ir/neg/neg2", listOf("runtime/runtime.c"), options())
        assert(result, "1.000000\n")
    }
}

class NegO1Tests: NegTest() {
    override fun options(): List<String> = listOf("-O1")
}

class NegO0Tests: NegTest() {
    override fun options(): List<String> = listOf()
}