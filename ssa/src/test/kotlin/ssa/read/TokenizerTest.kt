package read
import ir.read.*
import org.junit.jupiter.api.assertThrows
import kotlin.test.*

class TokenizerTest {
    @Test
    fun oneLineTest() {
        val tokenizer = Tokenizer("abs = add u64 3, %1")
        val iterator = tokenizer.iterator()
        assertEquals(Identifier("abs", 1,0), iterator.next())
        assertEquals(Equal(1, 4), iterator.next())
        assertEquals(Identifier("add", 1, 6), iterator.next())
        assertEquals(ElementaryTypeToken("u64", 1,10), iterator.next())
        assertEquals(IntValue(3, 1, 14), iterator.next())
        assertEquals(Comma(1, 15), iterator.next())
        assertEquals(ValueInstructionToken("1", 1, 17), iterator.next())
        assertEquals(iterator.hasNext(), false)
    }

    @Test
    fun severalLinesTest() {
        val tokenizer = Tokenizer("%3=gep ptr %129, %1\n%43=load ptr %3")
        val iterator = tokenizer.iterator()

        assertEquals(ValueInstructionToken("3", 1, 0), iterator.next())
        assertTrue(iterator.hasNext())
        assertEquals(Equal(1, 2), iterator.next())
        assertTrue(iterator.hasNext())
        assertEquals(Identifier("gep", 1, 3), iterator.next())
        assertEquals(ElementaryTypeToken("ptr", 1, 7), iterator.next())
        assertEquals(ValueInstructionToken("129", 1, 11), iterator.next())
        assertEquals(Comma(1, 15), iterator.next())
        assertEquals(ValueInstructionToken("1", 1, 17), iterator.next())
        assertEquals(ValueInstructionToken("43", 2, 0), iterator.next())
        assertEquals(Equal(2, 3), iterator.next())
        assertEquals(Identifier("load", 2, 4), iterator.next())
        assertEquals(ElementaryTypeToken("ptr", 2, 9), iterator.next())
        assertTrue(iterator.hasNext())
    }

    @Test
    fun readFloatTest() {
        val tokenizer = Tokenizer("3.45\nlabel:")
        val iterator = tokenizer.iterator()
        assertEquals(FloatValue(3.45, 1, 0), iterator.next())
        assertThrows<TokenizerException> {
            iterator.next()
        }
    }

    @Test
    fun readLabelTest() {
        val tokenizer = Tokenizer("3.45\nlabelName:")
        val iterator = tokenizer.iterator()
        assertEquals(FloatValue(3.45, 1, 0), iterator.next())
        assertEquals(LabelDefinition("labelName",2, 0), iterator.next())
    }

    @Test
    fun readIdentifierTest() {
        val tokenizer = Tokenizer("uval fval sval")
        val iterator = tokenizer.iterator()
        assertEquals(Identifier("uval", 1, 0), iterator.next())
        assertEquals(Identifier("fval", 1, 5), iterator.next())
        assertEquals(Identifier("sval", 1, 10), iterator.next())
    }

    @Test
    fun readIdentifier1Test() {
        val tokenizer = Tokenizer("3val")
        val iterator = tokenizer.iterator()
        assertEquals(Identifier("3val", 1, 0), iterator.next())
    }

    @Test
    fun readLabelUsageTest() {
        val tokenizer = Tokenizer("\nlabel %Lname")
        val iterator = tokenizer.iterator()
        assertEquals(LabelUsage("Lname", 2, 0), iterator.next())
    }

    @Test
    fun readFunctionName() {
        val tokenizer = Tokenizer("define void @some")
        val iterator = tokenizer.iterator()
        assertEquals(Define(1, 0), iterator.next())
        assertEquals(ElementaryTypeToken("void", 1, 7), iterator.next())
        assertEquals(FunctionName("some", 1, 13), iterator.next())
        assertFalse(iterator.hasNext())
    }

    @Test
    fun readStringWithDot() {
        val tokenizer = Tokenizer("for.Cond")
        val iterator = tokenizer.iterator()
        assertEquals(Identifier("for.Cond", 1, 0), iterator.next())
        assertFalse(iterator.hasNext())
    }

    @Test
    fun readStringWithDot1() {
        val tokenizer = Tokenizer(".for.Cond")
        val iterator = tokenizer.iterator()
        assertEquals(Dot(1, 0), iterator.next())
        assertEquals(Identifier("for.Cond", 1, 1), iterator.next())
        assertFalse(iterator.hasNext())
    }

    @Test
    fun readWithErrorTest() {
        assertThrows<TokenizerException> {
            val tokenizer = Tokenizer("3.val\n g")
            tokenizer.iterator().next()
        }

        assertThrows<TokenizerException> {
            val tokenizer = Tokenizer("3.0val")
            tokenizer.iterator().next()
        }

        assertThrows<TokenizerException> {
            val tokenizer = Tokenizer("3..0val")
            tokenizer.iterator().next()
        }
    }
}