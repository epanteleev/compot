package ssa.ir

import ir.types.*
import kotlin.test.Test
import kotlin.test.assertEquals


class StructTypeTest {
    @Test
    fun test1() {
        val structType = StructType("Point", arrayListOf(Type.I32, Type.I32))
        assertEquals(8, structType.sizeof())
        assertEquals(0, structType.offset(0))
        assertEquals(4, structType.offset(1))
    }

    @Test
    fun test2() {
        val structType = StructType("Point", arrayListOf(Type.I32, Type.I64))
        assertEquals(16, structType.sizeof())
    }

    @Test
    fun test3() {
        val pointType = StructType("Point", arrayListOf(Type.I32, Type.I64))
        val structType1 = StructType("Rect", arrayListOf(pointType, pointType))
        val structType2 = StructType("Rect", arrayListOf(pointType, Type.I64))
        val structType3 = StructType("Rect", arrayListOf(Type.I64, pointType))
        assertEquals(32, structType1.sizeof())
        assertEquals(24, structType2.sizeof())
        assertEquals( 32, structType3.sizeof())
    }

    @Test
    fun test4() {
        val structType = StructType("Point", arrayListOf(Type.I32, Type.I8))
        assertEquals(8, structType.sizeof())
    }

    @Test
    fun test5() {
        val structType = StructType("Point", arrayListOf(Type.I8, Type.I32))
        assertEquals(8, structType.sizeof())
    }

    @Test
    fun test6() {
        val structType = StructType("Point", arrayListOf(Type.I32, Type.I8))
        assertEquals(0, structType.offset(0))
        assertEquals(4, structType.offset(1))
    }

    @Test
    fun test7() {
        val structType = StructType("Point", arrayListOf(Type.I32, Type.Ptr))
        assertEquals(16, structType.sizeof())
        assertEquals(0, structType.offset(0))
        assertEquals(8, structType.offset(1))
    }
}