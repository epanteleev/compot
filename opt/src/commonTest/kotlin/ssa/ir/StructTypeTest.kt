package ssa.ir

import ir.types.*
import kotlin.test.Test
import kotlin.test.assertEquals


class StructTypeTest {
    @Test
    fun test1() {
        val structType = StructType.create("Point", arrayListOf(I32Type, I32Type))
        assertEquals(8, structType.sizeOf())
        assertEquals(0, structType.offset(0))
        assertEquals(4, structType.offset(1))
    }

    @Test
    fun test2() {
        val structType = StructType.create("Point", arrayListOf(I32Type, I64Type))
        assertEquals(16, structType.sizeOf())
    }

    @Test
    fun test3() {
        val pointType = StructType.create("Point", arrayListOf(I32Type, I64Type))
        val structType1 = StructType.create("Rect", arrayListOf(pointType, pointType))
        val structType2 = StructType.create("Rect", arrayListOf(pointType, I64Type))
        val structType3 = StructType.create("Rect", arrayListOf(I64Type, pointType))
        assertEquals(32, structType1.sizeOf())
        assertEquals(24, structType2.sizeOf())
        assertEquals( 24, structType3.sizeOf())
    }

    @Test
    fun test4() {
        val structType = StructType.create("Point", arrayListOf(I32Type, I8Type))
        assertEquals(8, structType.sizeOf())
    }

    @Test
    fun test5() {
        val structType = StructType.create("Point", arrayListOf(I8Type, I32Type))
        assertEquals(8, structType.sizeOf())
    }

    @Test
    fun test6() {
        val structType = StructType.create("Point", arrayListOf(I32Type, I8Type))
        assertEquals(0, structType.offset(0))
        assertEquals(4, structType.offset(1))
    }

    @Test
    fun test7() {
        val structType = StructType.create("Point", arrayListOf(I32Type, PtrType))
        assertEquals(16, structType.sizeOf())
        assertEquals(0, structType.offset(0))
        assertEquals(8, structType.offset(1))
    }

    @Test
    fun test8() {
        val structType = StructType.create("Vect", arrayListOf(F32Type, F32Type, F32Type))
        assertEquals(12, structType.sizeOf())
        assertEquals(0, structType.offset(0))
        assertEquals(4, structType.offset(1))
        assertEquals(8, structType.offset(2))
    }

    @Test
    fun test9() {
        val structType = StructType.create("Point", arrayListOf(I32Type, F32Type))
        assertEquals(8, structType.sizeOf())
        assertEquals(0, structType.offset(0))
        assertEquals(4, structType.offset(1))
    }

    @Test
    fun test10() {
        val pointType = StructType.create("Point", arrayListOf(I32Type, F64Type))
        val structType1 = StructType.create("Rect", arrayListOf(pointType, pointType))
        assertEquals(32, structType1.sizeOf())
    }

    @Test
    fun test11() {
        val structType = StructType.create("Point", arrayListOf(I64Type, I32Type))
        assertEquals(16, structType.sizeOf())
        assertEquals(0, structType.offset(0))
        assertEquals(8, structType.offset(1))
    }

    @Test
    fun test12() {
        val structType = StructType.create("Point", arrayListOf(I8Type, I8Type, ArrayType(I8Type, 3)))
        assertEquals(5, structType.sizeOf())
        assertEquals(2, structType.offset(2))
    }

    @Test
    fun test13() {
        val structType = StructType.create("HashMap", arrayListOf(PtrType, I32Type, I32Type))
        assertEquals(16, structType.sizeOf())
    }
}