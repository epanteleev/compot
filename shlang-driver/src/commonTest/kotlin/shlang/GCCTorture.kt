package shlang

import kotlin.test.*
import common.CommonCTest

sealed class GCCTorture : CommonCTest() {
    @Test
    @Ignore
    fun test_20001122_1() {
        val result = runCTest("shlang/gcc-c-torture/ieee/20001122-1", listOf(), options())
        assertEquals(0, result.exitCode)
    }

    @Test
    @Ignore
    fun test_20010226_1() {
        val result = runCTest("shlang/gcc-c-torture/ieee/20010226-1", listOf(), options())
        assertEquals(0, result.exitCode)
    }

    @Test
    @Ignore
    fun test_20011123_1() {
        val result = runCTest("shlang/gcc-c-torture/ieee/20011123-1", listOf(), options())
        assertEquals(0, result.exitCode)
    }

    @Test
    @Ignore
    fun test_acc1() {
        val result = runCTest("shlang/gcc-c-torture/ieee/acc1", listOf(), options())
        assertEquals(0, result.exitCode)
    }
}

class GCCTortureO0: GCCTorture() {
    override fun options(): List<String> = listOf()
}

class GCCTortureO1: GCCTorture() {
    override fun options(): List<String> = listOf("-O1")
}