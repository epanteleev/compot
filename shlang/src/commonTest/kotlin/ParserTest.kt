
import parser.CProgramParser
import parser.LineAgnosticAstPrinter
import parser.ParserException
import parser.nodes.AbstractDeclarator
import parser.nodes.AnyDeclarator
import parser.nodes.Declaration
import parser.nodes.DeclarationSpecifier
import parser.nodes.Expression
import parser.nodes.FunctionDeclarationNode
import parser.nodes.FunctionNode
import parser.nodes.Statement
import tokenizer.CTokenizer
import tokenizer.TokenList
import kotlin.test.*


class ParserTest {
    private fun apply(input: String): TokenList {
        return CTokenizer.apply(input, "<test-data>")
    }
    
    @Test
    fun testAssign() {
        val tokens = apply("t = 3 + 5;")
        val parser = CProgramParser.build(tokens)

        val expr = parser.assignment_expression() as Expression
        assertEquals("t = 3 + 5;", LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun testAssign2() {
        val tokens = apply("t = 3 + 5 * (67 << 56);")
        val parser = CProgramParser.build(tokens)
        val expr = parser.assignment_expression() as Expression
        assertEquals("t = 3 + 5 * 67 << 56;", LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun testAssign3() {
        val tokens = apply("t = (1 || 1 || (1 || 1 >= 56))")
        val parser = CProgramParser.build(tokens)
        val expr = parser.assignment_expression() as Expression
        assertEquals("t = 1 || 1 || 1 || 1 >= 56;", LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun testAssignBinary() {
        val tokens = apply("t *= 5")
        val parser = CProgramParser.build(tokens)

        val expr = parser.assignment_expression() as Expression
        assertEquals("t *= 5", LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun testConditional() {
        val tokens = apply("v = 0? a : b;")
        val parser = CProgramParser.build(tokens)

        val expr = parser.assignment_expression() as Expression
        assertEquals("v = 0? a : b;", LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun testDeclaration() {
        val tokens = apply("int t = 3 + 5;")
        val parser = CProgramParser.build(tokens)

        val expr = parser.declaration() as Declaration
        println(expr)
        assertEquals("int t = 3 + 5;", LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun testDeclaration1() {
        val tokens = apply("int bb, *aa;")
        val parser = CProgramParser.build(tokens)

        val expr = parser.declaration() as Declaration
        assertEquals("int bb, *aa;", LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun testDeclaration2() {
        val tokens = apply("int* bb;")
        val parser = CProgramParser.build(tokens)

        val expr = parser.declaration() as Declaration
        println(expr)
        assertEquals("int *bb;", LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun testDeclaration3() {
        val tokens = apply("int t, *a;")
        val parser = CProgramParser.build(tokens)

        val expr = parser.declaration() as Declaration
        println(expr)
        assertEquals("int t, *a;", LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun testDeclaration4() {
        val tokens = apply("int t = 90, *a = 0;")
        val parser = CProgramParser.build(tokens)

        val expr = parser.declaration() as Declaration
        println(expr)
        assertEquals("int t = 90, *a = 0;", LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_expr_stmt() {
        val tokens = apply("t = 3 + 5")
        val throwable = assertFails { CProgramParser.build(tokens).expression_statement() }
        assertTrue { throwable is ParserException }
    }

    @Test
    fun test_for_stmt() {
        val tokens = apply("for (a = 0; a < 5; a++) {  }")
        val parser = CProgramParser.build(tokens)

        val expr = parser.statement() as Statement
        assertEquals("for(a = 0;a < 5;a++) {}", LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_for_stmt1() {
        val tokens = apply("for (int a = 0; a < 5; a++) {  }")
        val parser = CProgramParser.build(tokens)

        val expr = parser.statement() as Statement
        assertEquals("for(int a = 0;a < 5;a++) {}", LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_for_stmt2() {
        val tokens = apply("for (; a < 5; a++) {  }")
        val parser = CProgramParser.build(tokens)

        val expr = parser.statement() as Statement
        assertEquals("for(;a < 5;a++) {}", LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_for_stmt3() {
        val tokens = apply("for (; a < 5;) {  }")
        val parser = CProgramParser.build(tokens)

        val expr = parser.statement() as Statement
        assertEquals("for(;a < 5;) {}", LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_for_stmt4() {
        val tokens = apply("for (;;) {  }")
        val parser = CProgramParser.build(tokens)

        val expr = parser.statement() as Statement
        assertEquals("for(;;) {}", LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_for_stmt_with_body() {
        val tokens = apply("for (int a = 0; a < 5; a++) { char tt = a + 4; }")
        val parser = CProgramParser.build(tokens)

        val expr = parser.statement() as Statement
        assertEquals("for(int a = 0;a < 5;a++) {char tt = a + 4;}", LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_for_stmt_with_body1() {
        val tokens = apply("for (int a = 0; a < 5; a++) { char tt = a + 4; int rt; }")
        val parser = CProgramParser.build(tokens)

        val expr = parser.statement() as Statement
        println(expr)
        val expected = "for(int a = 0;a < 5;a++) {char tt = a + 4; int rt;}"
        assertEquals(expected, LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_if_stmt() {
        val tokens = apply("if (a < 5) { char tt = a + 4; int rt; }")
        val parser = CProgramParser.build(tokens)

        val expr = parser.statement() as Statement
        println(expr)
        val expected = "if(a < 5) {char tt = a + 4; int rt;}"
        assertEquals(expected, LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_if_stmt1() {
        val tokens = apply("if (a < 5) { char tt = a + 4; int rt; } else { int rt; }")
        val parser = CProgramParser.build(tokens)

        val expr = parser.statement() as Statement
        println(expr)
        val expected = "if(a < 5) {char tt = a + 4; int rt;} else {int rt;}"
        assertEquals(expected, LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_if_stmt2() {
        val tokens = apply("if (a < 5) { char tt = a + 4; int rt; } else if (a < 6) { int rt; }")
        val parser = CProgramParser.build(tokens)

        val expr = parser.statement() as Statement
        println(expr)
        val expected = "if(a < 5) {char tt = a + 4; int rt;} else if(a < 6) {int rt;}"
        assertEquals(expected, LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_if_stmt3() {
        val tokens = apply("if (a < 5) { char tt = a + 4; int rt; } else if (a < 6) { int rt; } else { int rt; }")
        val parser = CProgramParser.build(tokens)

        val expr = parser.statement() as Statement
        println(expr)
        val expected = "if(a < 5) {char tt = a + 4; int rt;} else if(a < 6) {int rt;} else {int rt;}"
        assertEquals(expected, LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_while_stmt() {
        val tokens = apply("while (a < 5) { char tt = a + 4; int rt; }")
        val parser = CProgramParser.build(tokens)

        val expr = parser.statement() as Statement
        val expected = "while(a < 5) {char tt = a + 4; int rt;}"
        assertEquals(expected, LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_do_while_stmt() {
        val tokens = apply("do { char tt = a + 4; int rt; } while (a < 5);")
        val parser = CProgramParser.build(tokens)

        val expr = parser.statement() as Statement
        val expected = "do {char tt = a + 4; int rt;} while(a < 5);"
        assertEquals(expected, LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_goto_stmt() {
        val tokens = apply("goto L;")
        val parser = CProgramParser.build(tokens)

        val expr = parser.statement() as Statement
        val expected = "goto L;"
        assertEquals(expected, LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_switch_stmt() {
        val tokens = apply("switch (a) { case 1: break; case 2: break; default: break; }")
        val parser = CProgramParser.build(tokens)

        val expr = parser.statement() as Statement
        val expected = "switch(a) {case 1: break; case 2: break; default: break;}"
        assertEquals(expected, LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_continue_stmt() {
        val tokens = apply("while (a < 5) { char tt = a + 4; int rt; continue;}")
        val parser = CProgramParser.build(tokens)

        val expr = parser.statement() as Statement
        val expected = "while(a < 5) {char tt = a + 4; int rt; continue;}"
        assertEquals(expected, LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_break_stmt() {
        val tokens = apply("while (a < 5) { char tt = a + 4; int rt; break;}")
        val parser = CProgramParser.build(tokens)

        val expr = parser.statement() as Statement
        val expected = "while(a < 5) {char tt = a + 4; int rt; break;}"
        assertEquals(expected, LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_return_stmt() {
        val tokens = apply("while (a < 5) { char tt = a + 4; int rt; return 5;}")
        val parser = CProgramParser.build(tokens)

        val expr = parser.statement() as Statement
        val expected = "while(a < 5) {char tt = a + 4; int rt; return 5;}"
        assertEquals(expected, LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_function_decl() {
        val tokens = apply("int main() { return 5; }")
        val parser = CProgramParser.build(tokens)

        val expr = parser.function_definition() as FunctionNode
        println(expr)
        val expected = "int main() {return 5;}"
        assertEquals(expected, LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_function_decl1() {
        val tokens = apply("int main(void) { return 5; }")
        val parser = CProgramParser.build(tokens)

        val expr = parser.function_definition() as FunctionNode
        println(expr)
        val expected = "int main(void ) {return 5;}"
        assertEquals(expected, LineAgnosticAstPrinter.print(expr))
        assertEquals(expr.name(), "main")
    }

    @Test
    fun test_function_decl2() {
        val tokens = apply("int main(int a) { return 5; }")
        val parser = CProgramParser.build(tokens)

        val expr = parser.function_definition() as FunctionNode
        println(expr)
        val expected = "int main(int a) {return 5;}"
        assertEquals(expected, LineAgnosticAstPrinter.print(expr))
        assertEquals(expr.name(), "main")
    }

    @Test
    fun test_function_decl3() {
        val tokens = apply("int main(int a, int b) { return 5; }")
        val parser = CProgramParser.build(tokens)

        val expr = parser.function_definition() as FunctionNode
        val expected = "int main(int a, int b) {return 5;}"
        println(expr)
        assertEquals(expected, LineAgnosticAstPrinter.print(expr))
        assertEquals(expr.name(), "main")
    }

    @Test
    fun test_function_decl4() {
        val tokens = apply("void fun(int(fn)(void) ) { }")
        val parser = CProgramParser.build(tokens)

        val expr = parser.function_definition() as FunctionNode
        println(expr)
        val expected = "void fun(int (fn)(void )) {}"
        assertEquals(expected, LineAgnosticAstPrinter.print(expr))
        assertEquals(expr.name(), "fun")
    }

    @Test
    fun test_declaration_specifiers0() {
        val tokens = apply("int")
        val parser = CProgramParser.build(tokens)

        val expr = parser.declaration_specifiers() as DeclarationSpecifier
        assertEquals("int", LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_declaration_specifiers1(): Unit {
        val tokens = apply("volatile int restrict")
        val parser = CProgramParser.build(tokens)

        val expr = parser.declaration_specifiers() as DeclarationSpecifier
        assertEquals("volatile int restrict", LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_function_decl5() {
        val tokens = apply("void fun(int(*fn)(void)) { }")
        val parser = CProgramParser.build(tokens)

        val expr = parser.function_definition() as FunctionNode
        println(expr)
        val expected = "void fun(int (*fn)(void )) {}"
        assertEquals(expected, LineAgnosticAstPrinter.print(expr))
        assertEquals(expr.name(), "fun")
    }

    @Test
    fun test_direct_declarator4() {
        val tokens = apply("arr[idx]")
        val parser = CProgramParser.build(tokens)

        val expr = parser.declarator() as AnyDeclarator
        println(expr)
        val expected = "arr[idx]"
        assertEquals(expected, LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_direct_declarator5() {
        val tokens = apply("arr[idx + 1]")
        val parser = CProgramParser.build(tokens)

        val expr = parser.declarator() as AnyDeclarator
        println(expr)
        val expected = "arr[idx + 1]"
        assertEquals(expected, LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_direct_abstract_declarator1() {
        val tokens = apply("(int a)")
        val parser = CProgramParser.build(tokens)

        val expr = parser.abstract_declarator() as AbstractDeclarator
        println(expr)
        val expected = "(int a)"
        assertEquals(expected, LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_direct_abstract_declarator2() {
        val tokens = apply("(int a, int b)")
        val parser = CProgramParser.build(tokens)

        val expr = parser.abstract_declarator() as AbstractDeclarator
        println(expr)
        val expected = "(int a, int b)"
        assertEquals(expected, LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_direct_abstract_declarator3() {
        val tokens = apply("(int a)[23]")
        val parser = CProgramParser.build(tokens)

        val expr = parser.abstract_declarator() as AbstractDeclarator
        println(expr)
        val expected = "(int a) [23]"
        assertEquals(expected, LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_function_pointer() {
        val tokens = apply("int(fn)(void);")
        val parser = CProgramParser.build(tokens)

        val expr = parser.declaration() as Declaration
        println(expr)
        val expected = "int (fn)(void );"
        assertEquals(expected, LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_init_declarator0() {
        val tokens = apply("a = 5")
        val parser = CProgramParser.build(tokens)

        val expr = parser.init_declarator() as AnyDeclarator
        println(expr)
        val expected = "a = 5"
        assertEquals(expected, LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_init_declarator1() {
        val tokens = apply("*a = 5")
        val parser = CProgramParser.build(tokens)

        val expr = parser.init_declarator() as AnyDeclarator
        println(expr)
        val expected = "*a = 5"
        assertEquals(expected, LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_array_access() {
        val tokens = apply("int fun() { arr[5]; }")
        val parser = CProgramParser.build(tokens)

        val expr = parser.function_definition() as FunctionNode
        println(expr)
        val expected = "int fun() {arr[5]}"
        assertEquals(expected, LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_array_access1() {
        val tokens = apply("int fun() { arr[5][6]; }")
        val parser = CProgramParser.build(tokens)

        val expr = parser.function_definition() as FunctionNode
        println(expr)
        val expected = "int fun() {arr[5][6]}"
        assertEquals(expected, LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_array_access2() {
        val tokens = apply("int fun() { arr[5][6][7]; }")
        val parser = CProgramParser.build(tokens)

        val expr = parser.function_definition() as FunctionNode
        println(expr)
        val expected = "int fun() {arr[5][6][7]}"
        assertEquals(expected, LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_array_initializer_list() {
        val tokens = apply("int fun() { int arr[1] = {9}; }")
        val parser = CProgramParser.build(tokens)

        val expr = parser.function_definition() as FunctionNode
        println(expr)
        val expected = "int fun() {int arr[1] = {9};}"
        assertEquals(expected, LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_array_initializer_list1() {
        val tokens = apply("int fun() { int arr[2] = {9, 8}; }")
        val parser = CProgramParser.build(tokens)

        val expr = parser.function_definition() as FunctionNode
        println(expr)
        val expected = "int fun() {int arr[2] = {9, 8};}"
        assertEquals(expected, LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_array_initializer_list2() {
        val tokens = apply("int fun() { int arr[2][3] = {9, 8, 7, 6, 5, 4}; }")
        val parser = CProgramParser.build(tokens)

        val expr = parser.function_definition() as FunctionNode
        println(expr)
        val expected = "int fun() {int arr[2][3] = {9, 8, 7, 6, 5, 4};}"
        assertEquals(expected, LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_array_initializer_list3() {
        val tokens = apply("int fun() { int arr[] = {9}; }")
        val parser = CProgramParser.build(tokens)

        val expr = parser.function_definition() as FunctionNode
        println(expr)
        val expected = "int fun() {int arr[] = {9};}"
        assertEquals(expected, LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_array_initializer_list4() {
        val tokens = apply("int arr[1][3] = {{9, 8, 8},};")
        val parser = CProgramParser.build(tokens)

        val expr = parser.translation_unit()
        println(expr)
        val expected = "int arr[1][3] = {{9, 8, 8}};"
        assertEquals(expected, LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_cast() {
        val tokens = apply("(int) a")
        val parser = CProgramParser.build(tokens)

        val expr = parser.cast_expression() as Expression
        println(expr)
        val expected = "(int) a"
        assertEquals(expected, LineAgnosticAstPrinter.print(expr))
    }

    @Test
    fun test_external_declaration() {
        val tokens = apply("int fun() { return 6; };")
        val parser = CProgramParser.build(tokens)

        val expr = parser.external_declaration() as FunctionDeclarationNode
        println(expr)
        val expected = "int fun() {return 6;}"
        assertEquals(expected, LineAgnosticAstPrinter.print(expr.function))
    }

    @Test
    fun test2() {
        val tokens = apply("int main () { return 6; }")
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        assertEquals("int main() {return 6;}", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun test3() {
        val tokens = apply("extern int val; int main() { return 6; }")
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        println(program)
        println(LineAgnosticAstPrinter.print(program))
        assertEquals("extern int val; int main() {return 6;}", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun test4() {
        val tokens = apply("const int val = 50; int main() { return 6; }")
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        assertEquals("const int val = 50; int main() {return 6;}", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun test5() {
        val tokens = apply("int sumAndMul(int a, int b) { return 6 * a + b; }")
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        assertEquals("int sumAndMul(int a, int b) {return 6 * a + b;}", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun structDecl() {
        val input = """
            struct point {
                int a;
                int b;
            };
            
            int sum(struct point a) { return a.a + a.b; }
        """.trimIndent()
        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        println(program)
        assertEquals("struct point {int a; int b;} ; int sum(struct point a) {return a.a + a.b;}", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun multilineFun() {
        val input = """
            int sum(int a) {
              int r = a + 56;
              return a + 4 + r;
            }
        """.trimIndent()

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        println(program)
        assertEquals("int sum(int a) {int r = a + 56; return a + 4 + r;}", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun typedefStruct() {
        val input = """
            typedef struct point { int t; } P;
        """.trimIndent()

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        assertEquals("typedef struct point {int t;} P;", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun floatTest() {
        val input = "int sum(float a) { return a + 4.97; }"

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        println(program)
        assertEquals("int sum(float a) {return a + 4.97;}", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun helloWorldTest() {
        val input = "const char* str = \"Hello world\";\n void main() { printf(str); }"

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        println(program)
        assertEquals("const char *str = \"Hello world\"; void main() {printf(str)}", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun pointer3() {
        val input = "double***value;"

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        println(program)
        assertEquals("double ***value;", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun pointer() {
        val input = "double* value = NULL; "

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        assertEquals("double *value = NULL;", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun arrayDecl() {
        val input = "int arr[100];"

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        assertEquals("int arr[100];", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun arrayDecl1() {
        val input = "int arr[100][20];"

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        assertEquals("int arr[100][20];", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun arrayAccess() {
        val input = "void fn() { arr[1] = 90; }"

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        println(program)
        assertEquals("void fn() {arr[1] = 90;}", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun arrayAccess2() {
        val input = "void fn() { arr[1][2] = 90; }"

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        assertEquals("void fn() {arr[1][2] = 90;}", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun lvaluePointer() {
        val input = "void fn() { int* arr; *arr = 90; }"

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        println(program)
        assertEquals("void fn() {int *arr; *arr = 90;}", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun loopFor() {
        val input = "void fn() { for(int i = 0; i < 10; i++) {} }"

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        println(program)
        assertEquals("void fn() {for(int i = 0;i < 10;i++) {}}", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun loopWhile() {
        val input = "void fn() { while(true) {} }"

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        println(program)
        assertEquals("void fn() {while(true) {}}", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun loopWhile1() {
        val input = "void fn(int t) { while(t < 100) { t++; } }"

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        println(program)
        assertEquals("void fn(int t) {while(t < 100) {t++}}", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun goto1() {
        val input = "int square(int num) { goto L; L: return num * num; }"

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        println(program)
        assertEquals("int square(int num) {goto L; L: return num * num;}", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun goto2() {
        val input = "int square(int num) { goto L; int t; L: return num * num; }"

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        println(program)
        assertEquals("int square(int num) {goto L; int t; L: return num * num;}", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun enum() {
        val input = "enum Animal { CAT, DOG };"

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        println(program)
        assertEquals("enum Animal {CAT, DOG} ;", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun union() {
        val input = "union { int x; float fp; };"

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        println(program)
        assertEquals("union union.0 {int x; float fp;} ;", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun functionDecl() {
        val input = "int main(int** args, int argv); "

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        println(program)
        assertEquals("int main(int **args, int argv);", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun functionDecl1() {
        val input = "int main(); "

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        println(program)
        assertEquals("int main();", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun functionDecl2() {
        val input = "int main(void); "

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        println(program)
        assertEquals("int main(void );", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun functionDecl3() {
        val input = "int main(int a); "

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        assertEquals("int main(int a);", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun funtionDecl4() {
        val input = "static void print(int G[][3]);"

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        assertEquals("static void print(int G[][3]);", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun functionDeclVararg() {
        val input = "int printf(char* fmt, ...); "

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        println(program)
        assertEquals("int printf(char *fmt, ...);", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun typedef() {
        val input = "typedef int INT; "

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        println(program)
        assertEquals("typedef int INT;", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun typedef1() {
        val input = "typedef struct Point {int a; int b;} P; "

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        println(program)
        assertEquals("typedef struct Point {int a; int b;} P;", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun structDefinition() {
        val input = """
            struct point {
                int a;
                int b;
            };
        """.trimIndent()
        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        println(program)
        assertEquals("struct point {int a; int b;} ;", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun structAccess() {
        val input = """
            struct point {
                int a;
                int b;
            };
            
            int sum(struct point* a) { return a->a + a->b; }
        """.trimIndent()
        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        println(program)
        assertEquals("struct point {int a; int b;} ; int sum(struct point *a) {return a->a + a->b;}", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun structAccess1() {
        val input = """
            typedef struct point {
                int a;
                int b;
            } Point;
            
            int sum(Point* a) { return a->a + a->b; }
        """.trimIndent()
        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        assertEquals("typedef struct point {int a; int b;} Point; int sum(Point *a) {return a->a + a->b;}", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun test_sizeof() {
        val input = "int main(int a) { return sizeof(int); } "

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        assertEquals("int main(int a) {return sizeof(int);}", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun test_sizeof1() {
        val input = "int main(int a) { return sizeof 90; } "

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        assertEquals("int main(int a) {return sizeof(90);}", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun test_ret_void() {
        val input = "void fn(int a) { return; } "

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        assertEquals("void fn(int a) {return ;}", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun test_dereference() {
        val input = "void fn(int* a) { int val = *a; return val; } "

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        assertEquals("void fn(int *a) {int val = *a; return val;}", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun testCallRecursion() {
        val input = "int fib(int n) { if (n <= 1) return n; return fib(n - 1) + fib(n - 2); }"

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        assertEquals("int fib(int n) {if(n <= 1) {return n;} return fib(n - 1) + fib(n - 2);}", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun testDesignatedInitializer() {
        val input = "int arr[2] = { [0] = 1, [1] = 2 };"

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        assertEquals("int arr[2] = {[0] = 1, [1] = 2};", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun testDesignatedInitializer1() {
        val input = "int arr[2][3] = { [0][0] = 1, [1][2] = 2 };"

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        assertEquals("int arr[2][3] = {[0][0] = 1, [1][2] = 2};", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun testDesignatedInitializer2() {
        val input = "int arr[2][3] = { [0][0] = 1, [1][2] = 2, 3, 4 };"

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        assertEquals("int arr[2][3] = {[0][0] = 1, [1][2] = 2, 3, 4};", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun testDesignatedStructInitializer() {
        val input = """
            struct point {
                int x;
                int y;
            };
            
            struct point p = { .x = 1, .y = 2 };
        """.trimIndent()

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        assertEquals("struct point {int x; int y;} ; struct point p = {.x = 1, .y = 2};", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun testDesignatedStructInitializer1() {
        val input = """
            struct point {
                int x;
                int y;
            };
            
            struct point p = { .x = 1, 2 };
        """.trimIndent()

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        assertEquals("struct point {int x; int y;} ; struct point p = {.x = 1, 2};", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun testDesignatedStructArrayInitializer() {
        val input = """
            struct point {
                int x;
                int y;
            };
            
            struct point p[2] = { [0] = {.x = 1, .y = 2}, [1] = {.x = 3, .y = 4} };
        """.trimIndent()

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        assertEquals("struct point {int x; int y;} ; struct point p[2] = {[0] = {.x = 1, .y = 2}, [1] = {.x = 3, .y = 4}};", LineAgnosticAstPrinter.print(program))
    }

    @Test
    fun testCompoundLiteral() {
        val input = """
            typedef struct point {
                int x;
                int y;
            } Point;
            
            Point *p = &(Point){1, 1};
        """.trimIndent()

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        assertEquals("typedef struct point {int x; int y;} Point; Point *p = &(Point){1, 1};", LineAgnosticAstPrinter.print(program))
    }
}