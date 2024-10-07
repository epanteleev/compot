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
}

class LACCTestsO0: LACCTests() {
    override fun options(): List<String> = listOf()
}

class LACCTestsO1: LACCTests() {
    override fun options(): List<String> = listOf("-O1")
}