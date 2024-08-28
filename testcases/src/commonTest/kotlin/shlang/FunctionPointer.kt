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

    @Test
    fun testFunctionPointer1() {
        val result = runCTest("shlang/function_pointer/function_ptr1", listOf(), options())
        assertEquals("Value: 20", result.output)
    }

    @Test
    fun testFunctionPointer2() {
        val result = runCTest("shlang/function_pointer/function_ptr2", listOf(), options())
        assertEquals("Value: 10", result.output)
    }
}

class FunctionPointerO0: FunctionPointer() {
    override fun options(): List<String> = listOf()
}

class FunctionPointerO1: FunctionPointer() {
    override fun options(): List<String> = listOf("-O1")
}