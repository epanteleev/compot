package shlang

import common.CommonCTest
import kotlin.test.Test


abstract class FunTests: CommonCTest() {
    @Test
    fun test1() {
        val result = runCTest("shlang/fun/1", listOf(), options())
        assertReturnCode(result, 57)
    }

    @Test
    fun test2() {
        val result = runCTest("shlang/fun/2", listOf(), options())
        assertReturnCode(result, 0)
    }

    @Test
    fun test3() {
        val result = runCTest("shlang/fun/3", listOf(), options())
        assertReturnCode(result, 20)
    }

    @Test
    fun test4() {
        val result = runCTest("shlang/fun/4", listOf(), options())
        assertReturnCode(result, 0)
    }

    @Test
    fun test5() {
        val result = runCTest("shlang/fun/5", listOf(), options())
        assertReturnCode(result, 20)
    }

    @Test
    fun test6() {
        val result = runCTest("shlang/fun/6", listOf(), options())
        assertReturnCode(result, 10)
    }

    @Test
    fun test7() {
        val result = runCTest("shlang/fun/7", listOf(), options())
        assertReturnCode(result, 20)
    }

    @Test
    fun test8() {
        val result = runCTest("shlang/fun/8", listOf(), options())
        assertReturnCode(result, 60)
    }

    @Test
    fun test9() {
        val result = runCTest("shlang/fun/9", listOf(), options())
        assertReturnCode(result, 49)
    }

    @Test
    fun test10() {
        val result = runCTest("shlang/fun/10", listOf(), options())
        assertReturnCode(result, 0)
    }

    @Test
    fun test11() {
        val result = runCTest("shlang/fun/11", listOf(), options())
        assertReturnCode(result, 90)
    }

    @Test
    fun test12() {
        val result = runCTest("shlang/fun/12", listOf(), options())
        assertReturnCode(result, 90)
    }

    @Test
    fun test13() {
        val result = runCTest("shlang/fun/13", listOf(), options())
        assertReturnCode(result, 90)
    }

    @Test
    fun test14() {
        val result = runCTest("shlang/fun/14", listOf(), options())
        assertReturnCode(result, 90)
    }

    @Test
    fun test15() {
        val result = runCTest("shlang/fun/15", listOf(), options())
        assertReturnCode(result, 90)
    }

    @Test
    fun test16() {
        val result = runCTest("shlang/fun/16", listOf(), options())
        assertReturnCode(result, 90)
    }

    @Test
    fun test17() {
        val result = runCTest("shlang/fun/17", listOf(), options())
        assertReturnCode(result, 9)
    }

    @Test
    fun test18() {
        val result = runCTest("shlang/fun/18", listOf(), options())
        assertReturnCode(result, 9)
    }

    @Test
    fun test19() {
        val result = runCTest("shlang/fun/19", listOf(), options())
        assertReturnCode(result, 9)
    }

    @Test
    fun test20() {
        val result = runCTest("shlang/fun/20", listOf(), options())
        assertReturnCode(result, 9)
    }

    @Test
    fun test21() {
        val result = runCTest("shlang/fun/21", listOf(), options())
        assertReturnCode(result, 10)
    }

    @Test
    fun test22() {
        val result = runCTest("shlang/fun/22", listOf(), options())
        assertReturnCode(result, 4)
    }

    @Test
    fun test23() {
        val result = runCTest("shlang/fun/23", listOf(), options())
        assertReturnCode(result, 2)
    }

    @Test
    fun test24() {
        val result = runCTest("shlang/fun/24", listOf(), options())
        assertReturnCode(result, 2)
    }

    @Test
    fun test25() {
        val result = runCTest("shlang/fun/25", listOf(), options())
        assertReturnCode(result, 2)
    }
}

class FunTestsO0: FunTests() {
    override fun options(): List<String> = listOf()
}

class FunTestsO1: FunTests() {
    override fun options(): List<String> = listOf("-O1")
}