package ssa.collections

import common.intMapOf
import kotlin.test.Test
import kotlin.test.assertEquals


class IntMapTest {
    @Test
    fun test1() {
        val map = intMapOf<Int, Int>(4) { it }
        assertEquals(0, map.size)

        map[1] = 2
        assertEquals(1, map.size)
        assertEquals(2, map[1])
        for (v in map.values) {
            assertEquals(2, v)
        }

        for (k in map.keys) {
            assertEquals(1, k)
        }

        for ((k, v) in map.entries) {
            assertEquals(1, k)
            assertEquals(2, v)
        }
    }
}