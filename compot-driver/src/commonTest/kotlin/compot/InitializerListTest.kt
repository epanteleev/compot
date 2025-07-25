package compot

import common.CommonCTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals


abstract class InitializerListTest: CommonCTest() {
    @Test
    fun test0() {
        val result = runCTest("compot/initializerList/initializerList", listOf(), options())
        assertEquals("123", result.output)
    }

    @Test
    fun test1() {
        val result = runCTest("compot/initializerList/initializerList1", listOf(), options())
        assertEquals("point.x = 1, point.y = 2.000", result.output)
    }

    @Test
    fun test2() {
        val result = runCTest("compot/initializerList/initializerList2", listOf(), options())
        assertEquals("point.x = 1, point.y = 2.000", result.output)
    }

    @Test
    fun test3() {
        val result = runCTest("compot/initializerList/initializerList3", listOf(), options())
        assertEquals("123", result.output)
    }

    @Test
    fun test4() {
        val result = runCTest("compot/initializerList/initializerList4", listOf(), options())
        assertEquals("1 3 3 \n4 5 6 \n7 8 9 \n", result.output)
    }

    @Test
    fun test5() {
        val result = runCTest("compot/initializerList/initializerList5", listOf(), options())
        assertEquals("1 3 3 4 \n4 5 6 7 \n7 8 9 10 \n", result.output)
    }

    @Test
    fun test6() {
        val result = runCTest("compot/initializerList/initializerList6", listOf(), options())
        assertEquals("1 2 3 \n4 5 6 \n7 8 9 \n10 11 12 \n", result.output)
    }

    @Test
    fun test7() {
        val result = runCTest("compot/initializerList/initializerList7", listOf(), options())
        assertEquals("1 2 3 \n4 5 6 \n7 8 9 \n10 11 12 \n", result.output)
    }

    @Test
    fun test8() {
        val result = runCTest("compot/initializerList/initializerList8", listOf(), options())
        assertEquals("1 2 3 \n4 5 6 \n7 8 9 \n10 11 12 \n", result.output)
    }

    @Test
    fun test9() {
        val result = runCTest("compot/initializerList/initializerList9", listOf(), options())
        assertEquals("1 32 3 ", result.output)
    }

    @Test
    fun test10() {
        val result = runCTest("compot/initializerList/initializerList10", listOf(), options())
        assertEquals("10 0", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun test11() {
        val result = runCTest("compot/initializerList/initializerList11", listOf(), options())
        assertEquals("0 0", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun test12() {
        val result = runCTest("compot/initializerList/initializerList12", listOf(), options())
        assertEquals("0 0", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun test13() {
        val result = runCTest("compot/initializerList/initializerList13", listOf(), options())
        assertEquals("text1: \n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun test14() {
        val result = runCTest("compot/initializerList/initializerList14", listOf(), options())
        assertEquals("b", result.output)
        assertEquals(0, result.exitCode)
    }
}

class InitializerListTestsO0: InitializerListTest() {
    override fun options(): List<String> = listOf()
}

class InitializerListTestsO1: InitializerListTest() {
    override fun options(): List<String> = listOf("-O1")
}