import types.*
import typedesc.TypeDesc
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class CStructTypeTest {
    @Test
    fun test1() {
        val members = arrayListOf(FieldMember("x", TypeDesc.from(INT)), FieldMember("y", TypeDesc.from(INT)))
        val structType = CStructType("Point", members)
        assertEquals(8, structType.size())
        assertEquals(0, structType.offset(0))
        assertEquals(4, structType.offset(1))
        assertFalse { structType.hasFloatOnly(0, 4) }
        assertFalse { structType.hasFloatOnly(4, 8) }
    }

    @Test
    fun test2() {
        val members = arrayListOf(FieldMember("x", TypeDesc.from(INT)), FieldMember("y", TypeDesc.from(LONG)))
        val structType = CStructType("Point", members)
        assertEquals(16, structType.size())
    }

    @Test
    fun test3() {
        val pointType = CStructType("Point", arrayListOf(FieldMember("x", TypeDesc.from(INT)), FieldMember("y", TypeDesc.from(LONG))))
        val members1 = arrayListOf(FieldMember("p1", TypeDesc.from(pointType)), FieldMember("p2", TypeDesc.from(pointType)))
        val structType1 = CStructType("Rect", members1)

        val members2 = arrayListOf(FieldMember("p", TypeDesc.from(pointType)), FieldMember("y", TypeDesc.from(LONG)))
        val structType2 = CStructType("Rect", members2)
        val members3 = arrayListOf(FieldMember("x", TypeDesc.from(LONG)), FieldMember("p", TypeDesc.from(pointType)))
        val structType3 = CStructType("Rect", members3)
        assertEquals(32, structType1.size())
        assertEquals(24, structType2.size())
        assertEquals( 24, structType3.size())
    }

    @Test
    fun test4() {
        val members = arrayListOf(FieldMember("x", TypeDesc.from(INT)), FieldMember("y", TypeDesc.from(CHAR)))
        val structType = CStructType("Point", members)
        assertEquals(8, structType.size())
    }

    @Test
    fun test5() {
        val members = arrayListOf(
            FieldMember("x", TypeDesc.from(FLOAT)),
            FieldMember("y", TypeDesc.from(FLOAT)),
            FieldMember("z", TypeDesc.from(FLOAT))
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
            FieldMember("x", TypeDesc.from(CHAR)),
            FieldMember("y", TypeDesc.from(CHAR)),
            FieldMember("z", TypeDesc.from(CArrayType(TypeDesc.from(CHAR), 3)))
        )
        val structType = CStructType("Point", members)
        assertEquals(5, structType.size())
        assertEquals(2, structType.offset(2))
    }

    @Test
    fun test7() {
        val members = arrayListOf(
            FieldMember("x", TypeDesc.from(LONG)),
            FieldMember("y", TypeDesc.from(INT))
        )
        val structType = CStructType("Point", members)
        assertEquals(16, structType.size())
        assertEquals(0, structType.offset(0))
        assertEquals(8, structType.offset(1))
        assertFalse { structType.hasFloatOnly(0, 8) }
        assertFalse { structType.hasFloatOnly(0, 4) }
    }
}