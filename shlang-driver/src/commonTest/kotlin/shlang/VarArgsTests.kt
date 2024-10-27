package shlang

import common.CommonCTest
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class VarArgsTests: CommonCTest() {
    @Test
    fun test1() {
        val result = runCTest("shlang/varArgs/varArgs1", listOf(), options())
        assertEquals("1\n", result.output)
    }
}

class VarArgsTestsO0: VarArgsTests() {
    override fun options(): List<String> = listOf()
}

class VarArgsTestsO1: VarArgsTests() {
    override fun options(): List<String> = listOf("-O1")
}