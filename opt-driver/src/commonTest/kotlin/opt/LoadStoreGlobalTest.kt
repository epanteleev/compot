package opt

import common.CommonIrTest
import kotlin.test.Test


abstract class LoadStoreGlobalTest : CommonIrTest() {
    @Test
    fun testLoadStoreGlobal() {
        val result = runTest("opt_ir/load_global/load_global_var", listOf("runtime/runtime.c"), options())
        assert(result, "120\n")
    }

    @Test
    fun testLoadStoreGlobal1() {
        val result = runTest("opt_ir/load_global/load_global_var1", listOf("runtime/runtime.c"), options())
        assert(result, "abc 120\n")
    }

    @Test
    fun testLoadStoreGlobal2() {
        val result = runTest("opt_ir/load_global/load_global_var2", listOf("runtime/runtime.c"), options())
        assert(result, "-8\n-16\n-32\n-64\n8\n16\n32\n64\n")
    }

    @Test
    fun testLoadStoreGlobal3() {
        val result = runTest("opt_ir/load_global/load_global_var3", listOf("runtime/runtime.c"), options())
        assert(result, "120.000000\n140.000000\n")
    }

    @Test
    fun testLoadStoreGlobal4() {
        val result = runTest("opt_ir/load_global/load_store_global_var", listOf("runtime/runtime.c"), options())
        assert(result, "1000\n")
    }
}

class LoadStoreGlobalO1Tests: LoadStoreGlobalTest() {
    override fun options(): List<String> = listOf("-O1")
}

class LoadStoreGlobalO0Tests: LoadStoreGlobalTest() {
    override fun options(): List<String> = listOf()
}