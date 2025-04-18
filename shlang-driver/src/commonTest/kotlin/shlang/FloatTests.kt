package shlang

import common.CommonCTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class FloatTests: CommonCTest() {
    @Test
    fun test1() {
        val result = runCTest("shlang/float/float1", listOf(), options())
        assertEquals("f: 179769313486231570814527423731704356798070567525844996598917476803157260780028538760589558632766878171540458953514382464234321326889464182768467546703537516986049910576551282076245490090389328944075868508455133942304583236903222948165808559332123348274797826204144723168738177180919299881250404026184124858368.000000", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun test2() {
        val result = runCTest("shlang/float/float2", listOf(), options())
        assertEquals(0, result.exitCode)
    }

    @Test
    fun test3() {
        val result = runCTest("shlang/float/float3", listOf(), options())
        assertEquals("nan\n", result.output)
        assertEquals(1, result.exitCode)
    }

    @Test
    fun test4() {
        val result = runCTest("shlang/float/float4", listOf(), options())
        assertEquals("000000f87f\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun test5() {
        val result = runCTest("shlang/float/float5", listOf(), options())
        assertEquals("nan\n", result.output)
        assertEquals(1, result.exitCode)
    }

    @Test
    fun test6() {
        val result = runCTest("shlang/float/float6", listOf(), options())
        assertEquals(1, result.exitCode)
    }

    @Test
    fun test7() {
        val result = runCTest("shlang/float/float7", listOf(), options())
        assertEquals("-nan\n", result.output)
        assertEquals(1, result.exitCode)
    }

    @Test
    fun test8() {
        val result = runCTest("shlang/float/float8", listOf(), options())
        assertEquals(1, result.exitCode)
    }
}

class FloatTestsO0: FloatTests() {
    override fun options(): List<String> = listOf()
}

class FloatTestsO1: FloatTests() {
    override fun options(): List<String> = listOf("-O1")
}