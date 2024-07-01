package shlang

import common.CommonCTest
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class GlobalVar: CommonCTest() {
    @Test
    fun testGlobalVar() {
        val result = runCTest("shlang/global_var/global_var0", listOf(), options())
        assertEquals("100", result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testGlobalVar1() {
        val result = runCTest("shlang/global_var/global_var1", listOf(), options())
        assertEquals("89", result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testGlobalVar2() {
        val result = runCTest("shlang/global_var/global_var2", listOf(), options())
        assertEquals("100", result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testGlobalVar3() {
        val result = runCTest("shlang/global_var/global_var3", listOf(), options())
        assertEquals("Hello World!\n", result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testGlobalVar4() {
        val result = runCTest("shlang/global_var/global_var4", listOf(), options())
        assertEquals("Hello World!\n", result.output)
        assertReturnCode(result, 0)
    }
}

class GlobalVarO0: GlobalVar() {
    override fun options(): List<String> = listOf()
}

class GlobalVarO1: GlobalVar() {
    override fun options(): List<String> = listOf("-O1")
}