package shlang

import common.CommonCTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals


sealed class LACCTests : CommonCTest() {
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
    fun testArrayReverseIndex() {
        val result = runCTest("shlang/lacc/array-reverse-index", listOf(), options())
        assertEquals(2, result.exitCode)
    }

    @Test
    fun testArrayZeroLength() {
        val result = runCTest("shlang/lacc/array-zero-length", listOf(), options())
        val expect = """
            |{3, 2, 1}
            |size: 0, 3
            |""".trimMargin()
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

    @Test
    fun testBitwiseComplement() {
        val result = runCTest("shlang/lacc/bitwise-complement", listOf(), options())
        assertEquals(228, result.exitCode)
    }

    @Test
    fun testBitwiseConstant() {
        val result = runCTest("shlang/lacc/bitwise-constant", listOf(), options())
        assertEquals(79, result.exitCode)
    }

    @Test
    fun testBitwiseExpression() {
        val result = runCTest("shlang/lacc/bitwise-expression", listOf(), options())
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testBitwiseSignExtend() {
        val result = runCTest("shlang/lacc/bitwise-sign-extend", listOf(), options())
        val expect = "-7116886904016191872, 7116886904016191871, -1\n"
        assertEquals(expect, result.output)
        assertEquals(46, result.exitCode)
    }

    @Test
    fun testByteLoad() {
        val result = runCTest("shlang/lacc/byte-load", listOf(), options())
        assertEquals(101, result.exitCode)
    }

    @Test
    @Ignore
    fun testCastFloatUnion() {
        val result = runCTest("shlang/lacc/cast-float-union", listOf(), options())
        assertEquals("549754765312.000000\n", result.output)
        assertEquals(20, result.exitCode)
    }

    @Test
    fun testCastFloat() {
        val result = runCTest("shlang/lacc/cast-float", listOf(), options())
        assertEquals(1, result.exitCode)
    }

    @Test
    fun testCastFunctionArgs() {
        val result = runCTest("shlang/lacc/cast-function-args", listOf(), options())
        assertEquals("18446744073709551613, 253, 2.710000, 3.140000\n", result.output)
        assertEquals(46, result.exitCode)
    }

    @Test
    fun testCastFunction() {
        val result = runCTest("shlang/lacc/cast-function", listOf(), options())
        assertEquals("foo\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testCastImmediateTruncate() {
        val result = runCTest("shlang/lacc/cast-immediate-truncate", listOf(), options())
        assertEquals("44480, 254, -34\n", result.output)
        assertEquals(16, result.exitCode)
    }

    @Test
    fun testCast() {
        val result = runCTest("shlang/lacc/cast", listOf(), options())
        assertEquals(1, result.exitCode)
    }

    @Test
    fun testCommaSideEffects() {
        val result = runCTest("shlang/lacc/comma-side-effects", listOf(), options())
        assertEquals("453128631, 453128631\n", result.output)
        assertEquals(21, result.exitCode)
    }

    @Test
    fun testComment() {
        val result = runCTest("shlang/lacc/comment", listOf(), options())
        assertEquals("24, 24, w ??=*/ /*t???, a\"/*b\"\\\\, 34, 39\n", result.output)
        assertEquals(41, result.exitCode)
    }

    @Test
    fun testCompare() {
        val result = runCTest("shlang/lacc/compare", listOf(), options())
        assertEquals("1, 0, 0, 1\n", result.output)
        assertEquals(11, result.exitCode)
    }
}

class LACCTestsO0: LACCTests() {
    override fun options(): List<String> = listOf()
}

class LACCTestsO1: LACCTests() {
    override fun options(): List<String> = listOf("-O1")
}