package ssa.collections

import common.LListNode
import common.LeakedLinkedList
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals


class IntNode(val value: Int) : LListNode(){
    override fun prev(): LListNode? {
        return prev
    }

    override fun next(): LListNode? {
        return next
    }
}

class IntLeakedLinkedList : LeakedLinkedList<IntNode>()


fun leakedLinkedListOf(vararg elements: Int): IntLeakedLinkedList {
    val list = IntLeakedLinkedList()
    for (element in elements) {
        list.add(IntNode(element))
    }
    return list
}

class LeakedLinkedListTest {
    @Test
    fun testLeakedLinkedList() {
        val list = leakedLinkedListOf(1, 2, 3, 4, 5)
        assert(list.size == 5)
        assert(list[0].value == 1)
        assert(list[1].value == 2)
        assert(list[2].value == 3)
        assert(list[3].value == 4)
        assert(list[4].value == 5)
    }

    @Test
    fun testLeakedLinkedListContains() {
        val list = leakedLinkedListOf(1, 2, 3, 4, 5)
        assert(list.contains(list[0]))
        assert(list.contains(list[1]))
        assert(list.contains(list[2]))
        assert(list.contains(list[3]))
        assert(list.contains(list[4]))
    }

    @Test
    fun testLeakedLinkedListIndexOf() {
        val list = leakedLinkedListOf(1, 2, 3, 4, 5)
        assert(list.indexOf(list[0]) == 0)
        assert(list.indexOf(list[1]) == 1)
        assert(list.indexOf(list[2]) == 2)
        assert(list.indexOf(list[3]) == 3)
        assert(list.indexOf(list[4]) == 4)
    }

    @Test
    fun testLeakedLinkedListContainsAll() {
        val list = leakedLinkedListOf(1, 2, 3, 4, 5)
        assert(list.containsAll(list))
    }

    @Test
    fun testLeakedLinkedListRemove() {
        val list = leakedLinkedListOf(1, 2, 3, 4, 5)
        val node = list[2]
        list.remove(node)
        assert(list.size == 4)
        assert(!list.contains(node))
    }

    @Test
    fun testLeakedLinkedListIterator() {
        val list = leakedLinkedListOf(1, 2, 3, 4, 5)
        val iterator = list.iterator()
        assert(iterator.hasNext())
        assert(iterator.next().value == 1)
        assert(iterator.hasNext())
        assert(iterator.next().value == 2)
        assert(iterator.hasNext())
        assert(iterator.next().value == 3)
        assert(iterator.hasNext())
        assert(iterator.next().value == 4)
        assert(iterator.hasNext())
        assert(iterator.next().value == 5)
        assert(!iterator.hasNext())
    }

    @Test
    fun testLeakedLinkedListIsEmpty() {
        val list = leakedLinkedListOf(1, 2, 3, 4, 5)
        assert(!list.isEmpty())
        list.clear()
        assert(list.isEmpty())
    }

    @Test
    fun testLeakedLinkedListClear() {
        val list = leakedLinkedListOf(1, 2, 3, 4, 5)
        list.clear()
        assert(list.isEmpty())
    }

    @Test
    fun testLeakedLinkedListHashCode() {
        val list = leakedLinkedListOf(1, 2, 3, 4, 5)
        assert(list.hashCode() == list.hashCode())
    }

    @Test
    fun testConcurrentModification() {
        val list = leakedLinkedListOf(1, 2, 3, 4, 5)
        assertThrows<ConcurrentModificationException> {
            for (node in list) {
                if (node.value == 3) {
                    list.remove(node)
                }
            }
        }
    }

    @Test
    fun testTransform() {
        val list = leakedLinkedListOf(1, 2, 3, 4, 5)
        list.transform {
            val new = IntNode(90)
            list.addAfter(it, new)
            new
        }

        assertEquals(1, list[0].value)
        assertEquals(90, list[1].value)
        assertEquals(2, list[2].value)
        assertEquals(90, list[3].value)
        assertEquals(3, list[4].value)
        assertEquals(90, list[5].value)
        assertEquals(4, list[6].value)
        assertEquals(90, list[7].value)
        assertEquals(5, list[8].value)
    }
}