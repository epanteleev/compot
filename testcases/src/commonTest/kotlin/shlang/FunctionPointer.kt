package shlang

import common.CommonCTest
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class FunctionPointer: CommonCTest() {
    @Test
    fun testFunctionPointer0() {
        val result = runCTest("shlang/function_pointer/function_ptr0", listOf(), options())
        assertEquals("Hello", result.output)
    }
}

class FunctionPointerO0: FunctionPointer() {
    override fun options(): List<String> = listOf()
}

class FunctionPointerO1: FunctionPointer() {
    override fun options(): List<String> = listOf("-O1")
}