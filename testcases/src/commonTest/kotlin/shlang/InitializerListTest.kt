package shlang

import common.CommonCTest
import kotlin.test.Test
import kotlin.test.assertEquals


abstract class InitializerListTest: CommonCTest() {
    @Test
    fun test0() {
        val result = runCTest("shlang/initializerList/initializerList", listOf(), options())
        assertEquals("123", result.output)
    }

    @Test
    fun test1() {
        val result = runCTest("shlang/initializerList/initializerList1", listOf(), options())
        assertEquals("point.x = 1, point.y = 2.000", result.output)
    }

    @Test
    fun test2() {
        val result = runCTest("shlang/initializerList/initializerList2", listOf(), options())
        assertEquals("point.x = 1, point.y = 2.000", result.output)
    }

    @Test
    fun test3() {
        val result = runCTest("shlang/initializerList/initializerList3", listOf(), options())
        assertEquals("123", result.output)
    }

    @Test
    fun test4() {
        val result = runCTest("shlang/initializerList/initializerList4", listOf(), options())
        assertEquals("1 3 3 \n4 5 6 \n7 8 9 \n", result.output)
    }

    @Test
    fun test5() {
        val result = runCTest("shlang/initializerList/initializerList5", listOf(), options())
        assertEquals("1 3 3 4 \n4 5 6 7 \n7 8 9 10 \n", result.output)
    }

    @Test
    fun test6() {
        val result = runCTest("shlang/initializerList/initializerList6", listOf(), options())
        assertEquals("1 2 3 \n4 5 6 \n7 8 9 \n10 11 12 \n", result.output)
    }

    @Test
    fun test7() {
        val result = runCTest("shlang/initializerList/initializerList7", listOf(), options())
        assertEquals("1 2 3 \n4 5 6 \n7 8 9 \n10 11 12 \n", result.output)
    }

    @Test
    fun test8() {
        val result = runCTest("shlang/initializerList/initializerList8", listOf(), options())
        assertEquals("1 2 3 \n4 5 6 \n7 8 9 \n10 11 12 \n", result.output)
    }
}

class InitializerListTestsO0: InitializerListTest() {
    override fun options(): List<String> = listOf()
}

class InitializerListTestsO1: InitializerListTest() {
    override fun options(): List<String> = listOf("-O1")
}