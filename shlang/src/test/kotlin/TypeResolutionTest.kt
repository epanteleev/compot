
import parser.ProgramParser
import parser.nodes.*
import tokenizer.CTokenizer
import types.CPointerType
import types.CType
import types.TypeHolder
import kotlin.test.Test
import kotlin.test.assertEquals


class TypeResolutionTest {

    @Test
    fun testInt1() {
        val tokens = CTokenizer.apply("2 + 3")
        val parser = ProgramParser(tokens)

        val expr = parser.assignment_expression() as Expression
        val typeResolver = TypeHolder.default()
        val type = expr.resolveType(typeResolver)
        assertEquals(CType.INT, type)
    }

    @Test
    fun testFloat1() {
        val tokens = CTokenizer.apply("2.0 + 3.0")
        val parser = ProgramParser(tokens)

        val expr = parser.assignment_expression() as Expression
        val typeResolver = TypeHolder.default()
        val type = expr.resolveType(typeResolver)
        assertEquals(CType.DOUBLE, type)
    }

    @Test
    fun testDeclarationSpecifiers1() {
        val tokens = CTokenizer.apply("volatile int restrict")
        val parser = ProgramParser(tokens)

        val expr = parser.declaration_specifiers() as DeclarationSpecifier
        val typeResolver = TypeHolder.default()
        assertEquals("volatile restrict int", expr.resolveType(typeResolver).toString())
    }

    @Test
    fun testDeclarationSpecifiers2() {
        val tokens = CTokenizer.apply("volatile float restrict")
        val parser = ProgramParser(tokens)

        val expr = parser.declaration_specifiers() as DeclarationSpecifier
        val typeResolver = TypeHolder.default()
        assertEquals("volatile restrict float", expr.resolveType(typeResolver).toString())
    }

    @Test
    fun testDeclarationSpecifiers3() {
        val tokens = CTokenizer.apply("volatile struct point")
        val parser = ProgramParser(tokens)

        val expr = parser.declaration_specifiers() as DeclarationSpecifier
        val typeResolver = TypeHolder.default()
        assertEquals("volatile struct point", expr.resolveType(typeResolver).toString())
    }

    @Test
    fun testDecl() {
        val tokens = CTokenizer.apply("int a = 3 + 6;")
        val parser = ProgramParser(tokens)

        val expr = parser.declaration() as Declaration
        println(expr)
        val typeResolver = TypeHolder.default()
        expr.resolveType(typeResolver)
        assertEquals(CType.INT, typeResolver["a"])
    }

    @Test
    fun testDecl2() {
        val tokens = CTokenizer.apply("int a = 3 + 6, v = 90;")
        val parser = ProgramParser(tokens)

        val expr = parser.declaration() as Declaration
        println(expr)
        val typeResolver = TypeHolder.default()
        expr.resolveType(typeResolver)
        assertEquals(CType.INT, typeResolver["a"])
        assertEquals(CType.INT, typeResolver["v"])
    }

    @Test
    fun testDecl3() {
        val tokens = CTokenizer.apply("int a = 3 + 6, v = 90, *p = &v;")
        val parser = ProgramParser(tokens)

        val expr = parser.declaration() as Declaration
        println(expr)
        val typeResolver = TypeHolder.default()
        expr.resolveType(typeResolver)
        assertEquals(CType.INT, typeResolver["a"])
        assertEquals(CType.INT, typeResolver["v"])
        assertEquals(CPointerType(CType.INT), typeResolver["p"])
    }
}