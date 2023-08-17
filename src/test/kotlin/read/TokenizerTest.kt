package read
import ir.read.*
import kotlin.test.Test
import kotlin.test.assertEquals

class TokenizerTest {
    @Test
    fun oneLineTest() {
        val tokenizer = Tokenizer("abs = add u64 3, %1")
        val iterator = tokenizer.iterator()
        assertEquals(Identifier("abs", 0,0), iterator.next())
        assertEquals(Equal(0, 4), iterator.next())
        assertEquals(Identifier("add", 0, 6), iterator.next())
        assertEquals(TypeToken("u64", 0, 0,10), iterator.next())
        assertEquals(IntValue(3, 0, 14), iterator.next())
        assertEquals(Comma(0, 15), iterator.next())
        assertEquals(ValueToken("1", 0, 17), iterator.next())
        assertEquals(iterator.hasNext(), false)
    }

    @Test
    fun severalLinesTest() {
        val tokenizer = Tokenizer("%3=gep u64** %129, %1\n%43=load u64* %3")
        val iterator = tokenizer.iterator()

        assertEquals(ValueToken("3", 0, 0), iterator.next())
        assertEquals(Equal(0, 2), iterator.next())
        assertEquals(Identifier("gep", 0, 3), iterator.next())
        assertEquals(TypeToken("u64", 2, 0, 7), iterator.next())
        assertEquals(ValueToken("129", 0, 13), iterator.next())
        assertEquals(Comma(0, 17), iterator.next())
        assertEquals(ValueToken("1", 0, 19), iterator.next())
        assertEquals(ValueToken("43", 1, 0), iterator.next())
        assertEquals(Equal(1, 3), iterator.next())
        assertEquals(Identifier("load", 1, 4), iterator.next())
        assertEquals(TypeToken("u64", 1, 1, 9), iterator.next())
    }

    @Test
    fun readFloatTest() {
        val tokenizer = Tokenizer("3.45")
        val iterator = tokenizer.iterator()
        assertEquals(FloatValue(3.45, 0, 0), iterator.next())
    }

    @Test
    fun readIdentifierTest() {
        val tokenizer = Tokenizer("uval fval sval")
        val iterator = tokenizer.iterator()
        assertEquals(Identifier("uval", 0, 0), iterator.next())
        assertEquals(Identifier("fval", 0, 5), iterator.next())
        assertEquals(Identifier("sval", 0, 10), iterator.next())
    }

    @Test
    fun readIdentifier1Test() {
        val tokenizer = Tokenizer("3val")
        val iterator = tokenizer.iterator()
        assertEquals(Identifier("3val", 0, 0), iterator.next())
    }
}