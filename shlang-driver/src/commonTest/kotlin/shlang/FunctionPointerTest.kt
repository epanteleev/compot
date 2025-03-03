package shlang

import common.CommonCTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class FunctionPointerTest: CommonCTest() {
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

    @Test
    fun testFunctionPointer3() {
        val result = runCTest("shlang/function_pointer/function_ptr3", listOf(), options())
        assertEquals("Value: 20", result.output)
    }

    @Test
    fun testFunctionPointer4() {
        val result = runCTest("shlang/function_pointer/function_ptr4", listOf(), options())
        assertEquals("Value: 10", result.output)
    }

    @Test
    fun testFunctionPointer5() {
        val result = runCTest("shlang/function_pointer/function_ptr5", listOf(), options())
        assertEquals("Value: 10", result.output)
    }

    @Test
    fun testFunctionPointer6() {
        val result = runCTest("shlang/function_pointer/function_ptr6", listOf(), options())
        assertEquals("1\n2\n", result.output)
    }

    @Test
    fun testFunctionPointer7() {
        val result = runCTest("shlang/function_pointer/function_ptr7", listOf(), options())
        assertEquals("v=2\n", result.output)
    }

    @Test
    @Ignore
    fun testFunctionPointer8() {
        val result = runCTest("shlang/function_pointer/function_ptr8", listOf(), options())
        assertEquals("2\n", result.output)
    }
}

class FunctionPointerTestO0: FunctionPointerTest() {
    override fun options(): List<String> = listOf()
}

class FunctionPointerTestO1: FunctionPointerTest() {
    override fun options(): List<String> = listOf("-O1")
}