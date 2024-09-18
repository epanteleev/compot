package shlang

import common.CommonCTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals


abstract class HelloTests: CommonCTest() {
    @Test
    fun testHelloWorld() {
        val result = runCTest("shlang/hello_world/helloWorld", listOf(), options())
        assert(result, "Hello, World!\n")
    }

    @Test
    fun testHelloWorld0() {
        val result = runCTest("shlang/hello_world/hello_world", listOf(), options())
        assertEquals("Hello, World!\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testHelloWorld1() {
        val result = runCTest("shlang/hello_world/hello_world1", listOf(), options())
        assertEquals("Hello, World!\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testHelloWorld2() {
        val result = runCTest("shlang/hello_world/hello_world2", listOf(), options())
        assertEquals("Hello, World!\n", result.error)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testHelloWorld3() {
        val result = runCTest("shlang/hello_world/hello_world3", listOf(), options())
        assertEquals("Hello World!\n", result.error)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testHelloWorld4() {
        val result = runCTest("shlang/hello_world/hello_world4", listOf(), options())
        assertEquals("Hello World!\n", result.error)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testHelloWorld5() {
        val result = runCTest("shlang/hello_world/hello_world5", listOf(), options())
        assertEquals("Hello \"World!\"\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testHelloWorld6() {
        val result = runCTest("shlang/hello_world/hello_world6", listOf(), options())
        assertEquals("Hello World!\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testHelloWorld7() {
        val result = runCTest("shlang/hello_world/hello_world7", listOf(), options())
        assertEquals("Hello World!\n", result.output)
        assertEquals(0, result.exitCode)
    }
}

class HelloTestsO0: HelloTests() {
    override fun options(): List<String> = listOf()
}

class HelloTestsO1: HelloTests() {
    override fun options(): List<String> = listOf("-O1")
}