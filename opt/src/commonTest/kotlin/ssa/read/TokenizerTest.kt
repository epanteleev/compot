package ssa.read

import ir.read.*
import ir.read.tokens.*
import ir.types.Type
import kotlin.test.*


class TokenizerTest {
    @Test
    fun oneLineTest() {
        val tokenizer = Tokenizer("abs = add u64 3, %1")
        val iterator = tokenizer.iterator()
        assertEquals(Identifier("abs", 1,0), iterator.next())
        assertEquals(Equal(1, 4), iterator.next())
        assertEquals(Identifier("add", 1, 6), iterator.next())
        assertEquals(UnsignedIntegerTypeToken(Type.U64, 1,10), iterator.next())
        assertEquals(IntValue(3, 1, 14), iterator.next())
        assertEquals(Comma(1, 15), iterator.next())
        assertEquals(LocalValueToken("1", 1, 17), iterator.next())
        assertEquals(iterator.hasNext(), false)
    }

    @Test
    fun severalLinesTest() {
        val tokenizer = Tokenizer("%3=gep ptr %129, %1\n%43=load ptr %3")
        val iterator = tokenizer.iterator()

        assertEquals(LocalValueToken("3", 1, 0), iterator.next())
        assertTrue(iterator.hasNext())
        assertEquals(Equal(1, 2), iterator.next())
        assertTrue(iterator.hasNext())
        assertEquals(Identifier("gep", 1, 3), iterator.next())
        assertEquals(PointerTypeToken(1, 7), iterator.next())
        assertEquals(LocalValueToken("129", 1, 11), iterator.next())
        assertEquals(Comma(1, 15), iterator.next())
        assertEquals(LocalValueToken("1", 1, 17), iterator.next())
        assertEquals(LocalValueToken("43", 2, 0), iterator.next())
        assertEquals(Equal(2, 3), iterator.next())
        assertEquals(Identifier("load", 2, 4), iterator.next())
        assertEquals(PointerTypeToken(2, 9), iterator.next())
        assertTrue(iterator.hasNext())
    }

    @Test
    fun readFloatTest() {
        val tokenizer = Tokenizer("3.45\nlabel:")
        val iterator = tokenizer.iterator()
        assertEquals(FloatValue(3.45, 1, 0), iterator.next())
        val throwable = assertFails {
            iterator.next()
        }
        assertTrue { throwable is TokenizerException }
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
        assertEquals(VoidTypeToken(1, 7), iterator.next())
        assertEquals(SymbolValue("some", 1, 13), iterator.next())
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
        val throwable = assertFails {
            val tokenizer = Tokenizer("3.val\n g")
            tokenizer.iterator().next()
        }
        assertTrue { throwable is TokenizerException }

        val throwable2 = assertFails {
            val tokenizer = Tokenizer("3.0val")
            tokenizer.iterator().next()
        }
        assertTrue { throwable2 is TokenizerException }

        val throwable3 = assertFails {
            val tokenizer = Tokenizer("3..0val")
            tokenizer.iterator().next()
        }
        assertTrue { throwable3 is TokenizerException }

        val throwable4 = assertFails {
            val tokenizer = Tokenizer("\"sdfsdf")
            tokenizer.iterator().next()
        }
        assertTrue { throwable4 is EOFException }
    }
}