package shlang

import common.CommonCTest
import kotlin.test.Test
import kotlin.test.assertEquals


abstract class SortTest: CommonCTest() {
    @Test
    fun testInsertionSort() {
        val result = runCTest("shlang/sort/insertionSort", listOf("runtime/runtime.c"), options())
        assert(result, "11 12 22 25 34 64 \n")
    }

    @Test
    fun testBubbleSort() {
        val result = runCTest("shlang/sort/bubble_sort_int", listOf("runtime/runtime.c"), options())
        assert(result, "11 12 22 25 34 64 \n")
    }

    @Test
    fun testQuickSort() {
        val result = runCTest("shlang/sort/quickSort", listOf(), options())
        assert(result, "Original array: 19 17 15 12 16 18 4 11 13 \nSorted array: 4 11 12 13 15 16 17 18 19 ")
    }

    @Test
    fun testQuickSort1() {
        val options = options() + "-DDATATYPE=float" + "-DFMT=\"%.1f \""
        val result = runCTest("shlang/sort/quickSort", listOf(), options)
        assert(result, "Original array: 19.0 17.0 15.0 12.0 16.0 18.0 4.0 11.0 13.0 \nSorted array: 4.0 11.0 12.0 13.0 15.0 16.0 17.0 18.0 19.0 ")
    }

    @Test
    fun testQuickSort2() {
        val options = options() + "-DDATATYPE=double" + "-DFMT=\"%.1lf \""
        val result = runCTest("shlang/sort/quickSort", listOf(), options)
        assert(result, "Original array: 19.0 17.0 15.0 12.0 16.0 18.0 4.0 11.0 13.0 \nSorted array: 4.0 11.0 12.0 13.0 15.0 16.0 17.0 18.0 19.0 ")
    }

    @Test
    fun testQuickSort3() {
        val options = options() + "-DDATATYPE=char" + "-DFMT=\"%hhd \""
        val result = runCTest("shlang/sort/quickSort", listOf(), options)
        assert(result, "Original array: 19 17 15 12 16 18 4 11 13 \nSorted array: 4 11 12 13 15 16 17 18 19 ")
    }

    @Test
    fun testQuickSort4() {
        val options = options() + "-DDATATYPE=short" + "-DFMT=\"%hd \""
        val result = runCTest("shlang/sort/quickSort", listOf(), options)
        assert(result, "Original array: 19 17 15 12 16 18 4 11 13 \nSorted array: 4 11 12 13 15 16 17 18 19 ")
    }

    @Test
    fun testQuickSort5() {
        val options = options() + "-DDATATYPE=long" + "-DFMT=\"%ld \""
        val result = runCTest("shlang/sort/quickSort", listOf(), options)
        assert(result, "Original array: 19 17 15 12 16 18 4 11 13 \nSorted array: 4 11 12 13 15 16 17 18 19 ")
    }

    @Test
    fun testQuickSort6() {
        val options = options() + "-DDATATYPE='unsigned int'" + "-DFMT=\"%u \""
        val result = runCTest("shlang/sort/quickSort", listOf(), options)
        assert(result, "Original array: 19 17 15 12 16 18 4 11 13 \nSorted array: 4 11 12 13 15 16 17 18 19 ")
    }

    @Test
    fun testQuickSort7() {
        val options = options() + "-DDATATYPE='unsigned long'" + "-DFMT=\"%lu \""
        val result = runCTest("shlang/sort/quickSort", listOf(), options)
        assert(result, "Original array: 19 17 15 12 16 18 4 11 13 \nSorted array: 4 11 12 13 15 16 17 18 19 ")
    }

    @Test
    fun testQuickSort8() {
        val options = options() + "-DDATATYPE='unsigned char'" + "-DFMT=\"%hhu \""
        val result = runCTest("shlang/sort/quickSort", listOf(), options)
        assert(result, "Original array: 19 17 15 12 16 18 4 11 13 \nSorted array: 4 11 12 13 15 16 17 18 19 ")
    }

    @Test
    fun testHeapSort() {
        val result = runCTest("shlang/sort/heap_sort", listOf(), options())
        assertEquals("sdf", result.output)
        assertEquals(0, result.exitCode)
    }
}

class SortTestsO0: SortTest() {
    override fun options(): List<String> = listOf()
}

class SortTestsO1: SortTest() {
    override fun options(): List<String> = listOf("-O1")
}