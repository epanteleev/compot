package shlang

import common.CommonCTest
import kotlin.test.Test
import kotlin.test.assertEquals


abstract class SwitchTests: CommonCTest() {
    @Test
    fun testSwitch0() {
        val result = runCTest("shlang/switch/switch0", listOf(), options())
        assertEquals("All tests passed\n", result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testSwitch1() {
        val result = runCTest("shlang/switch/switch1", listOf(), options())
        assertEquals("All tests passed\n", result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testSwitch2() {
        val result = runCTest("shlang/switch/switch2", listOf(), options())
        assertEquals("All tests passed\n", result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testSwitch3() {
        val result = runCTest("shlang/switch/switch3", listOf(), options())
        assertEquals("All tests passed\n", result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testSwitch4() {
        val result = runCTest("shlang/switch/switch4", listOf(), options())
        assertEquals("All tests passed\n", result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testSwitch5() {
        val result = runCTest("shlang/switch/switch5", listOf(), options())
        assertEquals("All tests passed\n", result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testSwitch6() {
        val result = runCTest("shlang/switch/switch6", listOf(), options())
        assertEquals("Red\n", result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testSwitch7() {
        val result = runCTest("shlang/switch/switch7", listOf(), options())
        assertReturnCode(result, 0)
    }

    @Test
    fun testSwitch8() {
        val result = runCTest("shlang/switch/switch8", listOf(), options())
        assertReturnCode(result, 0)
    }

    @Test
    fun testSwitch9() {
        val result = runCTest("shlang/switch/switch9", listOf(), options())
        assertEquals("All tests passed\n", result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testSwitch10() {
        val result = runCTest("shlang/switch/switch10", listOf(), options())
        assertReturnCode(result, 0)
    }

    @Test
    fun testSwitch11() {
        val result = runCTest("shlang/switch/switch11", listOf(), options())
        assertReturnCode(result, 0)
    }

    @Test
    fun testSwitch12() {
        val result = runCTest("shlang/switch/switch12", listOf(), options())
        assertEquals("All tests passed\n", result.output)
        assertReturnCode(result, 0)
    }

    @Test
    fun testSwitch13() {
        val result = runCTest("shlang/switch/switch13", listOf(), options())
        assertReturnCode(result, 1)
    }
}


class SwitchTestsO0: SwitchTests() {
    override fun options(): List<String> = listOf()
}

class SwitchTestsO1: SwitchTests() {
    override fun options(): List<String> = listOf("-O1")
}