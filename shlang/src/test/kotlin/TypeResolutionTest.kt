
import parser.ProgramParser
import parser.nodes.*
import tokenizer.CTokenizer
import types.*
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

    @Test
    fun testDecl4() {
        val tokens = CTokenizer.apply("struct point* a = 0;")
        val parser = ProgramParser(tokens)

        val expr = parser.declaration() as Declaration
        println(expr)
        val typeResolver = TypeHolder.default()
        expr.resolveType(typeResolver)
        assertEquals("struct point*", typeResolver["a"].toString())
    }

    @Test
    fun testDecl5() {
        val tokens = CTokenizer.apply("struct point* a = 0, *b = 0;")
        val parser = ProgramParser(tokens)

        val expr = parser.declaration() as Declaration
        println(expr)
        val typeResolver = TypeHolder.default()
        expr.resolveType(typeResolver)
        assertEquals("struct point*", typeResolver["a"].toString())
        assertEquals("struct point*", typeResolver["b"].toString())
    }

    @Test
    fun testDecl6() {
        val tokens = CTokenizer.apply("int add(int a, int b) { return a + b; }")
        val parser = ProgramParser(tokens)
        val expr = parser.function_definition() as FunctionNode
        val typeResolver = TypeHolder.default()
        val fnType = expr.resolveType(typeResolver)

        assertEquals("int add(int, int)", fnType.toString())
        assertEquals(CType.INT, typeResolver["a"])
        assertEquals(CType.INT, typeResolver["b"])
    }

    @Test
    fun testDecl7() {
        val tokens = CTokenizer.apply("int add(void) { }")
        val parser = ProgramParser(tokens)
        val expr = parser.function_definition() as FunctionNode
        val typeResolver = TypeHolder.default()
        val fnType = expr.resolveType(typeResolver)

        assertEquals("int add(void)", fnType.toString())
    }

    @Test
    fun testFunctionPointerDeclaration() {
        val tokens = CTokenizer.apply("int (*add)(int, int) = 0;")
        val parser = ProgramParser(tokens)
        val expr = parser.declaration() as Declaration
        println(expr)
        val typeResolver = TypeHolder.default()
        expr.resolveType(typeResolver)

        assertEquals("int(*)(int, int)", typeResolver["add"].toString())
    }

    @Test
    fun testFunctionPointerDeclaration2() {
        val tokens = CTokenizer.apply("int (*add)(void) = 0, val = 999, *v = 0;")
        val parser = ProgramParser(tokens)
        val expr = parser.declaration() as Declaration
        val typeResolver = TypeHolder.default()
        expr.resolveType(typeResolver)

        assertEquals("int(*)(void)", typeResolver["add"].toString())
        assertEquals("int", typeResolver["val"].toString())
        assertEquals("int*", typeResolver["v"].toString())
    }

    @Test
    fun testStructDeclaration() {
        val tokens = CTokenizer.apply("struct point { int x; int y; };")
        val parser = ProgramParser(tokens)
        val expr = parser.declaration() as Declaration
        println(expr)
        val typeResolver = TypeHolder.default()
        expr.resolveType(typeResolver)

        assertEquals("struct point {int x;int y;}", typeResolver.getStructType("point").toString())
    }

    @Test
    fun testArrayDeclaration() {
        val tokens = CTokenizer.apply("int a[10];")
        val parser = ProgramParser(tokens)
        val expr = parser.declaration() as Declaration
        println(expr)
        val typeResolver = TypeHolder.default()
        expr.resolveType(typeResolver)

        assertEquals("int[10]", typeResolver["a"].toString())
    }
}