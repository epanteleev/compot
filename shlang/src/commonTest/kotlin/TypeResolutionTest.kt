
import parser.CProgramParser
import parser.LineAgnosticAstPrinter
import parser.nodes.*
import tokenizer.CTokenizer
import types.*
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class TypeResolutionTest {

    @Test
    fun testInt1() {
        val tokens = CTokenizer.apply("2 + 3")
        val parser = CProgramParser.build(tokens)

        val expr = parser.assignment_expression() as Expression
        val typeResolver = TypeHolder.default()
        val type = expr.resolveType(typeResolver)
        assertEquals(CType.INT, type)
    }

    @Test
    fun testFloat1() {
        val tokens = CTokenizer.apply("2.0 + 3.0")
        val parser = CProgramParser.build(tokens)

        val expr = parser.assignment_expression() as Expression
        val typeResolver = TypeHolder.default()
        val type = expr.resolveType(typeResolver)
        assertEquals(CType.DOUBLE, type)
    }

    @Test
    fun testDeclarationSpecifiers1() {
        val tokens = CTokenizer.apply("volatile int restrict")
        val parser = CProgramParser.build(tokens)

        val expr = parser.declaration_specifiers() as DeclarationSpecifier
        val typeResolver = TypeHolder.default()
        assertEquals("volatile restrict int", expr.resolveType(typeResolver).toString())
    }

    @Test
    fun testDeclarationSpecifiers2() {
        val tokens = CTokenizer.apply("volatile float restrict")
        val parser = CProgramParser.build(tokens)

        val expr = parser.declaration_specifiers() as DeclarationSpecifier
        val typeResolver = TypeHolder.default()
        assertEquals("volatile restrict float", expr.resolveType(typeResolver).toString())
    }

    @Test
    fun testDeclarationSpecifiers3() {
        val tokens = CTokenizer.apply("volatile struct point")
        val parser = CProgramParser.build(tokens)

        val expr = parser.declaration_specifiers() as DeclarationSpecifier
        val typeResolver = parser.typeHolder()
        assertEquals("volatile struct point", expr.resolveType(typeResolver).toString())
    }

    @Test
    fun testDecl() {
        val tokens = CTokenizer.apply("int a = 3 + 6;")
        val parser = CProgramParser.build(tokens)

        val expr = parser.declaration() as Declaration
        println(expr)
        val typeResolver = TypeHolder.default()
        expr.resolveType(typeResolver)
        assertEquals(CType.INT, typeResolver["a"])
    }

    @Test
    fun testDecl2() {
        val tokens = CTokenizer.apply("int a = 3 + 6, v = 90;")
        val parser = CProgramParser.build(tokens)

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
        val parser = CProgramParser.build(tokens)

        val expr = parser.declaration() as Declaration
        println(expr)
        val typeResolver = TypeHolder.default()
        expr.resolveType(typeResolver)

        assertTrue { typeResolver.containsVar("a") }
        assertTrue { typeResolver.containsVar("v") }
        assertTrue { typeResolver.containsVar("p") }
        assertEquals(CType.INT, typeResolver["a"])
        assertEquals(CType.INT, typeResolver["v"])
        assertEquals(CPointerType(CType.INT), typeResolver["p"])
    }

    @Test
    fun testDecl4() {
        val tokens = CTokenizer.apply("struct point* a = 0;")
        val parser = CProgramParser.build(tokens)

        val expr = parser.declaration() as Declaration
        println(expr)
        val typeResolver = TypeHolder.default()
        expr.resolveType(typeResolver)
        assertEquals("struct point*", typeResolver["a"].toString())
    }

    @Test
    fun testDecl5() {
        val tokens = CTokenizer.apply("struct point* a = 0, *b = 0;")
        val parser = CProgramParser.build(tokens)

        val expr = parser.declaration() as Declaration
        println(expr)
        val typeResolver = TypeHolder.default()
        expr.resolveType(typeResolver)
        assertTrue { typeResolver.containsVar("a") }
        assertTrue { typeResolver.containsVar("b") }
        assertEquals("struct point*", typeResolver["a"].toString())
        assertEquals("struct point*", typeResolver["b"].toString())
    }

    @Test
    fun testDecl6() {
        val tokens = CTokenizer.apply("int add(int a, int b) { return a + b; }")
        val parser = CProgramParser.build(tokens)
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
        val parser = CProgramParser.build(tokens)
        val expr = parser.function_definition() as FunctionNode
        val typeResolver = TypeHolder.default()
        val fnType = expr.resolveType(typeResolver)

        assertEquals("int add(void)", fnType.toString())
    }

    @Test
    fun testDecl8() {
        val tokens = CTokenizer.apply("int add(int (a)(int, int), int b) { }")
        val parser = CProgramParser.build(tokens)
        val expr = parser.function_definition() as FunctionNode
        val typeResolver = TypeHolder.default()
        val fnType = expr.resolveType(typeResolver)

        assertEquals("int add(int(*)(int, int), int)", fnType.toString())
        assertEquals("int(*)(int, int)", typeResolver["a"].toString())
        assertEquals(CType.INT, typeResolver["b"])
    }

    @Test
    fun testDecl9() {
        val tokens = CTokenizer.apply("int printf(const char* format, ...) { }")
        val parser = CProgramParser.build(tokens)
        val expr = parser.function_definition() as FunctionNode
        val typeResolver = TypeHolder.default()
        val fnType = expr.resolveType(typeResolver)

        assertEquals("int printf(const char*, ...)", fnType.toString())
    }

    @Test
    fun testFunctionPointerDeclaration() {
        val tokens = CTokenizer.apply("int (*add)(int, int) = 0;")
        val parser = CProgramParser.build(tokens)
        val expr = parser.declaration() as Declaration
        println(expr)
        val typeResolver = TypeHolder.default()
        expr.resolveType(typeResolver)

        assertEquals("int(*)(int, int)", typeResolver["add"].toString())
    }

    @Test
    fun testFunctionPointerDeclaration1() {
        val tokens = CTokenizer.apply("int (*add)(int, int);")
        val parser = CProgramParser.build(tokens)
        val expr = parser.declaration() as Declaration
        println(expr)
        val typeResolver = TypeHolder.default()
        expr.resolveType(typeResolver)

        assertEquals("int(*)(int, int)", typeResolver["add"].toString())
    }

    @Test
    fun testFunctionPointerDeclaration2() {
        val tokens = CTokenizer.apply("int (*add)(void) = 0, val = 999, *v = 0;")
        val parser = CProgramParser.build(tokens)
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
        val parser = CProgramParser.build(tokens)
        val expr = parser.declaration() as Declaration
        println(expr)
        val typeResolver = TypeHolder.default()
        expr.resolveType(typeResolver)

        assertEquals("struct point {int x;int y;}", typeResolver.getStructType("point").toString())
    }

    @Test
    fun testStructDeclaration1() {
        val tokens = CTokenizer.apply("struct point;")
        val parser = CProgramParser.build(tokens)
        val expr = parser.declaration() as Declaration
        val typeResolver = parser.typeHolder()
        expr.resolveType(typeResolver)
        assertEquals("struct point", typeResolver.getStructType("point").toString())
    }

    @Test
    fun testArrayDeclaration() {
        val tokens = CTokenizer.apply("int a[10];")
        val parser = CProgramParser.build(tokens)
        val expr = parser.declaration() as Declaration
        println(expr)
        val typeResolver = TypeHolder.default()
        expr.resolveType(typeResolver)

        val a = typeResolver["a"]
        assertEquals("int[10]", a.toString())
    }

    @Test
    fun testArrayDeclaration1() {
        val tokens = CTokenizer.apply("int a[5 + 5];")
        val parser = CProgramParser.build(tokens)
        val expr = parser.declaration() as Declaration
        println(expr)
        val typeResolver = TypeHolder.default()
        expr.resolveType(typeResolver)

        val a = typeResolver["a"]
        assertEquals("int[10]", a.toString())
    }

    @Test
    fun testArrayDeclaration2() {
        val tokens = CTokenizer.apply("int a[10], b[20];")
        val parser = CProgramParser.build(tokens)
        val expr = parser.declaration() as Declaration
        println(expr)
        val typeResolver = TypeHolder.default()
        expr.resolveType(typeResolver)

        val a = typeResolver["a"]
        val b = typeResolver["b"]
        assertEquals("int[10]", a.toString())
        assertEquals("int[20]", b.toString())
    }

    @Test
    fun testArrayDeclaration3() {
        val tokens = CTokenizer.apply("int a[10], b[20], *c = 0;")
        val parser = CProgramParser.build(tokens)
        val expr = parser.declaration() as Declaration
        println(expr)
        val typeResolver = TypeHolder.default()
        expr.resolveType(typeResolver)

        val a = typeResolver["a"]
        val b = typeResolver["b"]
        val c = typeResolver["c"]
        assertEquals("int[10]", a.toString())
        assertEquals("int[20]", b.toString())
        assertEquals("int*", c.toString())
    }

    @Test
    fun testArrayDeclaration4() {
        val tokens = CTokenizer.apply("int a[10][30];")
        val parser = CProgramParser.build(tokens)
        val expr = parser.declaration() as Declaration
        println(expr)
        val typeResolver = TypeHolder.default()
        expr.resolveType(typeResolver)

        val a = typeResolver["a"]
        assertEquals("int[10][30]", a.toString())
    }

    @Test
    fun testConstString() {
        val tokens = CTokenizer.apply("const char* a = \"hello\";")
        val parser = CProgramParser.build(tokens)
        val expr = parser.declaration() as Declaration
        val typeResolver = TypeHolder.default()
        expr.resolveType(typeResolver)

        val a = typeResolver["a"]
        assertEquals("const char*", a.toString())
    }

    // https://port70.net/~nsz/c/c11/n1570.html#6.7.2.3p11
    @Test
    fun testTypedef1() {
        val input = """
          typedef struct tnode TNODE;
          struct tnode {
                int count;
                TNODE *left, *right;
          };
          TNODE s, *sp;
        """.trimIndent()
        val tokens = CTokenizer.apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        println(program)
        assertEquals("typedef struct tnode TNODE; struct tnode {int count; TNODE *left, *right;} ; TNODE s, *sp;", LineAgnosticAstPrinter.print(program))

        val typeHolder = parser.typeHolder()
        assertEquals("struct tnode {int count;struct tnode* left;struct tnode* right;}", typeHolder.getStructType("tnode").toString())
    }

    // https://port70.net/~nsz/c/c11/n1570.html#6.7.3p12
    @Test
    fun testTypedef2() {
        val input = """
          typedef int A[2][3];
          const A a = {{4, 5, 6}, {7, 8, 9}};
        """.trimIndent()
        val tokens = CTokenizer.apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        assertEquals("typedef int A[2][3]; const A a = {{4, 5, 6}, {7, 8, 9}};", LineAgnosticAstPrinter.print(program))
        assertEquals("int[2][3]", parser.typeHolder().getTypeOrNull("A").toString())
    }

    @Ignore
    fun testTypedef3() {
        val input = """
          typedef struct s1 { int x; } t1, *tp1;
          typedef struct s2 { int x; } t2, *tp2;
        """.trimIndent()
        val tokens = CTokenizer.apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        assertEquals("typedef struct s1 {int x;} t1, *tp1; typedef struct s2 {int x;} t2, *tp2;", LineAgnosticAstPrinter.print(program))
        val typeHolder = parser.typeHolder()
        assertEquals("struct s1 {int x;}", typeHolder.getStructType("s1").toString())
        assertEquals("struct s2 {int x;}", typeHolder.getStructType("s2").toString())
        assertEquals("struct s1 {int x;}", typeHolder.getStructType("t1").toString())
       // assertTrue { typeHolder.getStructType("tp1") is CPointerType } TODO
    }
}