
import parser.LineAgnosticAstPrinter
import parser.ProgramParser
import parser.nodes.*
import tokenizer.CTokenizer
import types.CType
import types.TypeHolder
import types.TypeResolver
import kotlin.test.Test
import kotlin.test.assertEquals

class TypeResolutionTest {

    @Test
    fun testInt1() {
        val tokens = CTokenizer.apply("2 + 3")
        val parser = ProgramParser(tokens)

        val expr = parser.assignment_expression() as Expression
        println(expr)
        val typeResolver = TypeHolder.default()
        val type = expr.resolveType(typeResolver)
        assertEquals(CType.INT, type)
    }

    @Test
    fun testFloat1() {
        val tokens = CTokenizer.apply("2.0 + 3.0")
        val parser = ProgramParser(tokens)

        val expr = parser.assignment_expression() as Expression
        println(expr)
        val typeResolver = TypeHolder.default()
        val type = expr.resolveType(typeResolver)
        assertEquals(CType.DOUBLE, type)
    }

    @Test
    fun testDeclarationSpecifiers1(): Unit {
        val tokens = CTokenizer.apply("volatile int restrict")
        val parser = ProgramParser(tokens)

        val expr = parser.declaration_specifiers() as DeclarationSpecifier
        println(expr)
        assertEquals("volatile int restrict", expr.ctype().toString())
    }

    @Test
    fun testDecl() {
        val tokens = CTokenizer.apply("int a = 3 + 6;")
        val parser = ProgramParser(tokens)

        val expr = parser.declaration() as Declaration
        println(expr)
        //val typeResolver = TypeHolder.default()
        //val type = expr.resolveType(typeResolver)
        //assertEquals(CType.DOUBLE, type)
    }
}