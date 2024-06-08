package ssa.collections

import common.identityHashMapOf
import kotlin.test.Test
import kotlin.test.assertEquals

class IdentityHashMapTest {

    @Test
    fun foreachEntries() {
        val map = identityHashMapOf<Int, String>()
        map[3] = "vvv"
        map[4] = "ffff"
        val entries = map.entries.sortedBy { it.key }
        assertEquals(entries[0].value, "vvv")
        assertEquals(entries[1].value, "ffff")
    }
}