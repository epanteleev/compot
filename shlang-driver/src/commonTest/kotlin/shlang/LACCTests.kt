package shlang

import common.CommonCTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals


abstract class LACCTests : CommonCTest() {
    @Test
    fun testAddressDerefOffset() {
        val result = runCTest("shlang/lacc/address-deref-offset", listOf(), options())
        assertEquals(4, result.exitCode)
    }

    @Test
    @Ignore
    fun testAnonymousMembers() {
        val result = runCTest("shlang/lacc/anonymous-members", listOf(), options())
        assertEquals(0, result.exitCode)
    }

    @Test
    @Ignore
    fun testAnonymousStruct() {
        val result = runCTest("shlang/lacc/anonymous-struct", listOf(), options())
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testArrayDecay() {
        val result = runCTest("shlang/lacc/array-decay", listOf(), options())
        assertEquals("2, 2\n", result.output)
        assertEquals(5, result.exitCode)
    }

    @Test
    fun testArrayNestedInit() {
        val result = runCTest("shlang/lacc/array-nested-init", listOf(), options())
        assertEquals(96, result.exitCode)
    }

    @Test
    fun testArrayParam() {
        val result = runCTest("shlang/lacc/array-param", listOf(), options())
        val expected = """
            |5, 7, 4 (13)
            |1, 2, 0 (3)
            |""".trimMargin()
        assertEquals(expected, result.output)
        assertEquals(12, result.exitCode)
    }

    @Test
    fun testArrayRegisters() {
        val result = runCTest("shlang/lacc/array-registers", listOf(), options())
        assertEquals("eax\n", result.output)
        assertEquals(4, result.exitCode)
    }

    @Test
    @Ignore
    fun testArrayReverseIndex() {
        val result = runCTest("shlang/lacc/array-reverse-index", listOf(), options())
        assertEquals(2, result.exitCode)
    }

    @Test
    @Ignore
    fun testArrayZeroLength() {
        val result = runCTest("shlang/lacc/array-zero-length", listOf(), options())
        val expect = """
            |{3, 2, 1}
            |size: 0, 3""".trimMargin()
        assertEquals(expect, result.output)
        assertEquals(11, result.exitCode)
    }

    @Test
    fun testArray() {
        val result = runCTest("shlang/lacc/array", listOf(), options())
        assertEquals(5, result.exitCode)
    }

    @Test
    fun testAssignDerefFloat() {
        val result = runCTest("shlang/lacc/assign-deref-float", listOf(), options())
        val expected = "3.140000, 2.710000\n"
        assertEquals(expected, result.output)
        assertEquals(19, result.exitCode)
    }

    @Test
    fun testAssignmentType() {
        val result = runCTest("shlang/lacc/assignment-type", listOf(), options())
        assertEquals("-1\n", result.output)
        assertEquals(0, result.exitCode)
    }
}

class LACCTestsO0: LACCTests() {
    override fun options(): List<String> = listOf()
}

class LACCTestsO1: LACCTests() {
    override fun options(): List<String> = listOf("-O1")
}