package compot

import common.CommonCTest
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class CListTest: CommonCTest()  {
    @Test
    fun testList0() {
        val result = runCTest("compot/list/listTest0", listOf(), options() + "-Isrc/resources/compot/list")
        assertEquals("1\n-2\n-3\n4\n5\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testListLong() {
        val opt = options() + "-Isrc/resources/compot/list" + "-DDATATYPE=long" + "-DFMT='\"%ld\\n\"'"
        val result = runCTest("compot/list/listTest0", listOf(), opt)
        assertEquals("1\n-2\n-3\n4\n5\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testListChar() {
        val opt = options() + "-Isrc/resources/compot/list" + "-DDATATYPE=char" + "-DFMT='\"%hhd\\n\"'"
        val result = runCTest("compot/list/listTest0", listOf(), opt)
        assertEquals("1\n-2\n-3\n4\n5\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testListShort() {
        val opt = options() + "-Isrc/resources/compot/list" + "-DDATATYPE=short" + "-DFMT='\"%hd\\n\"'"
        val result = runCTest("compot/list/listTest0", listOf(), opt)
        assertEquals("1\n-2\n-3\n4\n5\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testListDouble() {
        val opt = options() + "-Isrc/resources/compot/list" + "-DDATATYPE=double" + "-DFMT='\"%lf\\n\"'"
        val result = runCTest("compot/list/listTest0", listOf(), opt)
        assertEquals("1.000000\n-2.000000\n-3.000000\n4.000000\n5.000000\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testListFloat() {
        val opt = options() + "-Isrc/resources/compot/list" + "-DDATATYPE=float" + "-DFMT='\"%f\\n\"'"
        val result = runCTest("compot/list/listTest0", listOf(), opt)
        assertEquals("1.000000\n-2.000000\n-3.000000\n4.000000\n5.000000\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testList1() {
        val result = runCTest("compot/list/listTest1", listOf(), options() + "-Isrc/resources/compot/list")
        assertEquals("2\n4\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testListLong1() {
        val opt = options() + "-Isrc/resources/compot/list" + "-DDATATYPE=long" + "-DFMT='\"%ld\\n\"'"
        val result = runCTest("compot/list/listTest1", listOf(), opt)
        assertEquals("2\n4\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testListUnsignedInt1() {
        val opt = options() + "-Isrc/resources/compot/list" + "-DDATATYPE='unsigned int'" + "-DFMT='\"%u\\n\"'"
        val result = runCTest("compot/list/listTest1", listOf(), opt)
        assertEquals("2\n4\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testListUnsignedLong1() {
        val opt = options() + "-Isrc/resources/compot/list" + "-DDATATYPE='unsigned long'" + "-DFMT='\"%lu\\n\"'"
        val result = runCTest("compot/list/listTest1", listOf(), opt)
        assertEquals("2\n4\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testListUnsignedChar1() {
        val opt = options() + "-Isrc/resources/compot/list" + "-DDATATYPE='unsigned short'" + "-DFMT='\"%hu\\n\"'"
        val result = runCTest("compot/list/listTest1", listOf(), opt)
        assertEquals("2\n4\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testListUnsignedShort1() {
        val opt = options() + "-Isrc/resources/compot/list" + "-DDATATYPE='unsigned char'" + "-DFMT='\"%hhu\\n\"'"
        val result = runCTest("compot/list/listTest1", listOf(), opt)
        assertEquals("2\n4\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testListFloat2() {
        val opt = options() + "-Isrc/resources/compot/list" + "-DDATATYPE=float" + "-DFMT='\"%f\\n\"'"
        val result = runCTest("compot/list/listTest2", listOf(), opt)
        assertEquals("newNode->data: 6.700000\n", result.output)
        assertEquals(0, result.exitCode)
    }
}

class CListTestO0: CListTest() {
    override fun options(): List<String> = listOf()
}

class CListTestO1: CListTest() {
    override fun options(): List<String> = listOf("-O1")
}