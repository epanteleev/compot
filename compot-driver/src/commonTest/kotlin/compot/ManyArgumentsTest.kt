package compot

import common.CommonCTest
import kotlin.test.Test
import kotlin.test.assertEquals


abstract class ManyArgumentsTest: CommonCTest() {
    @Test
    fun testManyArguments1() {
        val options = options()
        val result = runCTest("compot/manyArguments", listOf(), options)
        assertEquals("13", result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testManyArguments2() {
        val options = options() + "-DVALUE_TYPE=short" + "-DVALUE_FMT=\"%hd\""
        val result = runCTest("compot/manyArguments", listOf(), options)
        assertEquals("13", result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testManyArguments3() {
        val options = options() + "-DVALUE_TYPE=char" + "-DVALUE_FMT=\"%hhd\""
        val result = runCTest("compot/manyArguments", listOf(), options)
        assertEquals("13", result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testManyArguments4() {
        val options = options() + "-DVALUE_TYPE=long" + "-DVALUE_FMT=\"%ld\""
        val result = runCTest("compot/manyArguments", listOf(), options)
        assertEquals("13", result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testManyArguments5() {
        val options = options() + "-DVALUE_TYPE=float" + "-DVALUE_FMT=\"%.3f\""
        val result = runCTest("compot/manyArguments", listOf(), options)
        assertEquals("13.000", result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testManyArguments6() {
        val options = options() + "-DVALUE_TYPE=double" + "-DVALUE_FMT=\"%.3lf\""
        val result = runCTest("compot/manyArguments", listOf(), options)
        assertEquals("13.000", result.output)
        assertReturnCode(result, 0)
    }
}

class ManyArgumentsTestO0: ManyArgumentsTest() {
    override fun options(): List<String> = listOf()
}

class ManyArgumentsTestO1: ManyArgumentsTest() {
    override fun options(): List<String> = listOf("-O1")
}