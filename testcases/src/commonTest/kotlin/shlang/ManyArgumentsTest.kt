package shlang

import common.CommonCTest
import kotlin.test.Test

abstract class ManyArgumentsTest: CommonCTest() {
    @Test
    fun testGlobalVar2Short() {
        val options = options()
        val result = runCTest("shlang/manyArguments", listOf(), options)
        //assertEquals("100", result.output)
        assertReturnCode(result, 0)
    }
}

class ManyArgumentsTestO0: ManyArgumentsTest() {
    override fun options(): List<String> = listOf()
}

class ManyArgumentsTestO1: ManyArgumentsTest() {
    override fun options(): List<String> = listOf("-O1")
}