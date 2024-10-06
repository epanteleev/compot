import typedesc.TypeDesc
import types.CArrayType
import types.CHAR
import types.CStructType
import types.FLOAT
import types.INT
import types.LONG
import types.Member
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CStructTypeTest {
    @Test
    fun test1() {
        val members = arrayListOf(Member("x", TypeDesc.from(INT)), Member("y", TypeDesc.from(INT)))
        val structType = CStructType("Point", members)
        assertEquals(8, structType.size())
        assertEquals(0, structType.offset(0))
        assertEquals(4, structType.offset(1))
        assertFalse { structType.hasFloatOnly(0, 4) }
        assertFalse { structType.hasFloatOnly(4, 8) }
    }

    @Test
    fun test2() {
        val members = arrayListOf(Member("x", TypeDesc.from(INT)), Member("y", TypeDesc.from(LONG)))
        val structType = CStructType("Point", members)
        assertEquals(16, structType.size())
    }

    @Test
    fun test3() {
        val pointType = CStructType("Point", arrayListOf(Member("x", TypeDesc.from(INT)), Member("y", TypeDesc.from(LONG))))
        val members1 = arrayListOf(Member("p1", TypeDesc.from(pointType)), Member("p2", TypeDesc.from(pointType)))
        val structType1 = CStructType("Rect", members1)

        val members2 = arrayListOf(Member("p", TypeDesc.from(pointType)), Member("y", TypeDesc.from(LONG)))
        val structType2 = CStructType("Rect", members2)
        val members3 = arrayListOf(Member("x", TypeDesc.from(LONG)), Member("p", TypeDesc.from(pointType)))
        val structType3 = CStructType("Rect", members3)
        assertEquals(32, structType1.size())
        assertEquals(24, structType2.size())
        assertEquals( 24, structType3.size())
    }

    @Test
    fun test4() {
        val members = arrayListOf(Member("x", TypeDesc.from(INT)), Member("y", TypeDesc.from(CHAR)))
        val structType = CStructType("Point", members)
        assertEquals(8, structType.size())
    }

    @Test
    fun test5() {
        val members = arrayListOf(
            Member("x", TypeDesc.from(FLOAT)),
            Member("y", TypeDesc.from(FLOAT)),
            Member("z", TypeDesc.from(FLOAT))
        )
        val structType = CStructType("Vect", members)
        assertEquals(12, structType.size())
        assertEquals(0, structType.offset(0))
        assertEquals(4, structType.offset(1))
        assertEquals(8, structType.offset(2))
        assertTrue { structType.hasFloatOnly(0, 4) }
        assertTrue { structType.hasFloatOnly(0, 8) }
    }

    @Test
    fun test6() {
        val members = arrayListOf(
            Member("x", TypeDesc.from(CHAR)),
            Member("y", TypeDesc.from(CHAR)),
            Member("z", TypeDesc.from(CArrayType(TypeDesc.from(CHAR), 3)))
        )
        val structType = CStructType("Point", members)
        assertEquals(5, structType.size())
        assertEquals(2, structType.offset(2))
    }

    @Test
    fun test7() {
        val members = arrayListOf(
            Member("x", TypeDesc.from(LONG)),
            Member("y", TypeDesc.from(INT))
        )
        val structType = CStructType("Point", members)
        assertEquals(16, structType.size())
        assertEquals(0, structType.offset(0))
        assertEquals(8, structType.offset(1))
        assertFalse { structType.hasFloatOnly(0, 8) }
        assertFalse { structType.hasFloatOnly(0, 4) }
    }
}