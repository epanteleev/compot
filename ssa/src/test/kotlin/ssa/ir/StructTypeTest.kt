package ssa.ir

import ir.types.*
import kotlin.test.Test
import kotlin.test.assertEquals


class StructTypeTest {
    @Test
    fun test1() {
        val structType = StructType("Point", arrayListOf(Type.I32, Type.I32))
        assertEquals(structType.size(), 8)
    }

    @Test
    fun test2() {
        val structType = StructType("Point", arrayListOf(Type.I32, Type.I64))
        assertEquals(structType.size(), 16)
    }

    @Test
    fun test3() {
        val pointType = StructType("Point", arrayListOf(Type.I32, Type.I64))
        val structType1 = StructType("Rect", arrayListOf(pointType, pointType))
        val structType2 = StructType("Rect", arrayListOf(pointType, Type.I64))
        val structType3 = StructType("Rect", arrayListOf(Type.I64, pointType))
        assertEquals(structType1.size(), 32)
        assertEquals(structType2.size(), 24)
        assertEquals(structType3.size(), 32)
    }
}