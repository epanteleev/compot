
import types.*
import parser.CProgramParser
import parser.LineAgnosticAstPrinter
import parser.nodes.*
import tokenizer.CTokenizer
import tokenizer.TokenList
import typedesc.StorageClass
import typedesc.TypeHolder
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class TypeResolutionTest {
    private fun apply(input: String): TokenList {
        return CTokenizer.apply(input, "<test-data>")
    }
    
    @Test
    fun testInt1() {
        val tokens = apply("2 + 3")
        val parser = CProgramParser.build(tokens)

        val expr = parser.assignment_expression() as Expression
        val typeResolver = TypeHolder.default()
        val type = expr.resolveType(typeResolver)
        assertEquals(INT, type)
    }

    @Test
    fun testFloat1() {
        val tokens = apply("2.0 + 3.0")
        val parser = CProgramParser.build(tokens)

        val expr = parser.assignment_expression() as Expression
        val typeResolver = TypeHolder.default()
        val type = expr.resolveType(typeResolver)
        assertEquals(DOUBLE, type)
    }

    @Test
    fun testDeclarationSpecifiers1() {
        val tokens = apply("volatile int restrict")
        val parser = CProgramParser.build(tokens)

        val expr = parser.declaration_specifiers() as DeclarationSpecifier
        val typeResolver = TypeHolder.default()
        assertEquals("volatile restrict int", expr.specifyType(typeResolver).toString())
    }

    @Test
    fun testDeclarationSpecifiers2() {
        val tokens = apply("volatile float restrict")
        val parser = CProgramParser.build(tokens)

        val expr = parser.declaration_specifiers() as DeclarationSpecifier
        val typeResolver = TypeHolder.default()
        assertEquals("volatile restrict float", expr.specifyType(typeResolver).toString())
    }

    @Test
    fun testDeclarationSpecifiers3() {
        val tokens = apply("volatile struct point")
        val parser = CProgramParser.build(tokens)

        val expr = parser.declaration_specifiers() as DeclarationSpecifier
        val typeResolver = parser.typeHolder()
        assertEquals("volatile struct point", expr.specifyType(typeResolver).toString())
    }

    @Test
    fun testDecl() {
        val tokens = apply("int a = 3 + 6;")
        val parser = CProgramParser.build(tokens)

        val expr = parser.declaration() as Declaration
        val typeResolver = TypeHolder.default()
        val vars = expr.declareVars(typeResolver)
        val a = vars.find { it.name == "a" }!!

        assertEquals(INT, a.typeDesc.cType())
    }

    @Test
    fun testDecl2() {
        val tokens = apply("int a = 3 + 6, v = 90;")
        val parser = CProgramParser.build(tokens)

        val expr = parser.declaration() as Declaration
        val typeResolver = TypeHolder.default()
        val vars = expr.declareVars(typeResolver)
        val a = vars.find { it.name == "a" }!!
        val v = vars.find { it.name == "v" }!!

        assertEquals(INT, a.typeDesc.cType())
        assertEquals(INT, v.typeDesc.cType())
    }

    @Test
    fun testDecl3() {
        val tokens = apply("int a = 3 + 6, v = 90, *p = &v;")
        val parser = CProgramParser.build(tokens)

        val expr = parser.declaration() as Declaration
        val typeResolver = TypeHolder.default()
        val vars = expr.declareVars(typeResolver)
        val a = vars.find { it.name == "a" }!!
        val v = vars.find { it.name == "v" }!!
        val p = vars.find { it.name == "p" }!!

        assertEquals(INT, a.typeDesc.cType())
        assertEquals(INT, v.typeDesc.cType())
        assertEquals(CPointer(INT), p.typeDesc.cType())
    }

    @Test
    fun testDecl4() {
        val tokens = apply("struct point* a = 0;")
        val parser = CProgramParser.build(tokens)

        val expr = parser.declaration() as Declaration
        val typeResolver = TypeHolder.default()
        val vars = expr.declareVars(typeResolver)
        val a = vars.find { it.name == "a" }!!
        assertEquals("struct point*", a.toString())
    }

    @Test
    fun testDecl5() {
        val tokens = apply("struct point* a = 0, *b = 0;")
        val parser = CProgramParser.build(tokens)

        val expr = parser.declaration() as Declaration
        val typeResolver = TypeHolder.default()
        val vars = expr.declareVars(typeResolver)
        val a = vars.find { it.name == "a" }!!
        val b = vars.find { it.name == "b" }!!

        assertEquals("struct point*", a.toString())
        assertEquals("struct point*", b.toString())
    }

    @Test
    @Ignore
    fun testDecl6() {
        val tokens = apply("int add(int a, int b) { return a + b; }")
        val parser = CProgramParser.build(tokens)
        val expr = parser.function_definition() as FunctionNode
        val typeResolver = TypeHolder.default()
        val fnType = expr.resolveType(typeResolver)

        assertEquals("int add(int, int)", fnType.toString())
        assertEquals(INT, typeResolver["a"].typeDesc.cType())
        assertEquals(INT, typeResolver["b"].typeDesc.cType())
    }

    @Test
    fun testDecl7() {
        val tokens = apply("int add(void) { }")
        val parser = CProgramParser.build(tokens)
        val expr = parser.function_definition() as FunctionNode
        val typeResolver = TypeHolder.default()
        val fnType = expr.resolveType(typeResolver)

        assertEquals("int add()", fnType.toString())
    }

    @Test
    @Ignore
    fun testDecl8() {
        val tokens = apply("int add(int (a)(int, int), int b) { }")
        val parser = CProgramParser.build(tokens)
        val expr = parser.function_definition() as FunctionNode
        val typeResolver = TypeHolder.default()
        val fnType = expr.resolveType(typeResolver)

        assertEquals("int add(int a(int, int), int)", fnType.toString())
        assertEquals("int a(int, int)", typeResolver["a"].toString())
        assertEquals(INT, typeResolver["b"].typeDesc.cType())
    }

    @Test
    fun testDecl9() {
        val tokens = apply("int printf(const char* format, ...) { }")
        val parser = CProgramParser.build(tokens)
        val expr = parser.function_definition() as FunctionNode
        val typeResolver = TypeHolder.default()
        val fnType = expr.resolveType(typeResolver)

        assertEquals("int printf(const char*, ...)", fnType.toString())
    }

    @Test
    fun testFunctionPointerDeclaration() {
        val tokens = apply("int (*add)(int, int) = 0;")
        val parser = CProgramParser.build(tokens)
        val expr = parser.declaration() as Declaration
        val typeResolver = TypeHolder.default()
        val vars = expr.declareVars(typeResolver)
        val add = vars.find { it.name == "add" }!!

        assertEquals("int(int, int)*", add.toString())
    }

    @Test
    fun testFunctionPointerDeclaration1() {
        val tokens = apply("int (*add)(int, int);")
        val parser = CProgramParser.build(tokens)
        val expr = parser.declaration() as Declaration
        val typeResolver = TypeHolder.default()

        val vars = expr.declareVars(typeResolver)
        val add = vars.find { it.name == "add" }!!

        assertEquals("int(int, int)*", add.toString())
    }

    @Test
    fun testFunctionPointerDeclaration2() {
        val tokens = apply("int (*add)(void) = 0, val = 999, *v = 0;")
        val parser = CProgramParser.build(tokens)
        val expr = parser.declaration() as Declaration
        val typeResolver = TypeHolder.default()
        val vars = expr.declareVars(typeResolver)

        val add = vars.find { it.name == "add" }!!
        val valVar = vars.find { it.name == "val" }!!
        val v = vars.find { it.name == "v" }!!

        assertEquals("int()*", add.toString())
        assertEquals("int", valVar.toString())
        assertEquals("int*", v.toString())
    }

    @Test
    fun testFunctionDeclarator() {
        val tokens = apply("int b(int b), n(float f);")
        val parser = CProgramParser.build(tokens)
        parser.translation_unit()
        val typeResolver = parser.typeHolder()

        assertEquals("int b(int)", typeResolver.getFunctionType("b").toString())
        assertEquals("int n(float)", typeResolver.getFunctionType("n").toString())
    }

    @Test
    fun testStructDeclaration() {
        val tokens = apply("struct point { int x; int y; };")
        val parser = CProgramParser.build(tokens)
        val expr = parser.declaration() as Declaration

        val typeResolver = TypeHolder.default()
        expr.declareVars(typeResolver)

        assertEquals("struct point {int x;int y;}", typeResolver.getStructType("point").toString())
    }

    @Test
    fun testStructDeclaration1() {
        val tokens = apply("struct point;")
        val parser = CProgramParser.build(tokens)
        val expr = parser.declaration() as Declaration
        val typeResolver = parser.typeHolder()
        expr.declareVars(typeResolver)
        assertEquals("struct point", typeResolver.getStructType("point").toString())
    }

    @Test
    fun testArrayDeclaration() {
        val tokens = apply("int a[10];")
        val parser = CProgramParser.build(tokens)
        val expr = parser.declaration() as Declaration

        val typeResolver = TypeHolder.default()
        val vars = expr.declareVars(typeResolver)

        val a = vars.find { it.name == "a" }!!
        assertEquals("[10]int", a.toString())
    }

    @Test
    fun testArrayDeclaration1() {
        val tokens = apply("int a[5 + 5];")
        val parser = CProgramParser.build(tokens)
        val expr = parser.declaration() as Declaration

        val typeResolver = TypeHolder.default()
        val vars = expr.declareVars(typeResolver)

        val a = vars.find { it.name == "a" }!!
        assertEquals("[10]int", a.toString())
    }

    @Test
    fun testArrayDeclaration2() {
        val tokens = apply("int a[10], b[20];")
        val parser = CProgramParser.build(tokens)
        val expr = parser.declaration() as Declaration

        val typeResolver = TypeHolder.default()
        val vars = expr.declareVars(typeResolver)

        val a = vars.find { it.name == "a" }!!
        val b = vars.find { it.name == "b" }!!
        assertEquals("[10]int", a.toString())
        assertEquals("[20]int", b.toString())
    }

    @Test
    fun testArrayDeclaration3() {
        val tokens = apply("int a[10], b[20], *c = 0;")
        val parser = CProgramParser.build(tokens)
        val expr = parser.declaration() as Declaration

        val typeResolver = TypeHolder.default()
        val vars = expr.declareVars(typeResolver)

        val a = vars.find { it.name == "a" }!!
        val b = vars.find { it.name == "b" }!!
        val c = vars.find { it.name == "c" }!!
        assertEquals("[10]int", a.toString())
        assertEquals("[20]int", b.toString())
        assertEquals("int*", c.toString())
    }

    @Test
    fun testArrayDeclaration4() {
        val tokens = apply("int a[10][30];")
        val parser = CProgramParser.build(tokens)
        val expr = parser.declaration() as Declaration

        val typeResolver = TypeHolder.default()
        val vars = expr.declareVars(typeResolver)

        val a = vars.find { it.name == "a" }!!
        assertEquals("[10][30]int", a.toString())
    }

    @Test
    fun testConstString() {
        val tokens = apply("const char* a = \"hello\";")
        val parser = CProgramParser.build(tokens)
        val expr = parser.declaration() as Declaration
        val typeResolver = TypeHolder.default()
        val vars = expr.declareVars(typeResolver)

        val a = vars.find { it.name == "a" }!!
        assertEquals("const char*", a.toString())
    }

    @Test
    fun testUnsignedInt() {
        val tokens = apply("unsigned int a = 10;")
        val parser = CProgramParser.build(tokens)
        val expr = parser.declaration() as Declaration
        val typeResolver = parser.typeHolder()
        val vars = expr.declareVars(typeResolver)

        val a = vars.find { it.name == "a" }!!
        assertEquals(UINT, a.typeDesc.cType())
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
        val tokens = apply(input)
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
        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        assertEquals("typedef int A[2][3]; const A a = {{4, 5, 6}, {7, 8, 9}};", LineAgnosticAstPrinter.print(program))
        assertEquals("[2][3]int", parser.typeHolder().getTypedef("A").toString())
    }

    @Test
    fun testTypedef3() {
        val input = """
          typedef struct s1 { int x; } t1, *tp1;
          typedef struct s2 { int x; } t2, *tp2;
        """.trimIndent()
        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        assertEquals("typedef struct s1 {int x;} t1, *tp1; typedef struct s2 {int x;} t2, *tp2;", LineAgnosticAstPrinter.print(program))
        val typeHolder = parser.typeHolder()
        assertEquals("struct s1 {int x;}", typeHolder.getStructType("s1").toString())
        assertEquals("struct s2 {int x;}", typeHolder.getStructType("s2").toString())
        assertEquals("struct s1 {int x;}", typeHolder.getTypedef("t1").toString())
        assertTrue { typeHolder.getTypedef("tp1").cType() is CPointer }
    }

    @Test
    fun testAnonMember() {
        val input = """
            |union B {
            | int a;
            | struct {
            |    int b;
            |    char c;
            | };
            |};
            |union B b;
        """.trimMargin()
        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        parser.translation_unit()
        val typeHolder = parser.typeHolder()
        val unionType = typeHolder.getVarTypeOrNull("b") ?: error("Cannot find union type")
        assertEquals("union B {int a;struct struct.1 {int b;char c;}}", unionType.toString())
        val ty = unionType.typeDesc.cType() as CUnionType
        assertEquals(0, ty.fieldByNameOrNull("a")?.index)
        assertEquals(0, ty.fieldByNameOrNull("b")?.index)
        //assertEquals(2, ty.fieldIndex("c")) TODO
    }

    @Test
    fun testAnonMember1() {
        val input = """
            |struct A {
            | int a;
            | union {
            |    int b;
            |    char c;
            | };
            |};
            |struct A a;
        """.trimMargin()
        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        parser.translation_unit()
        val typeHolder = parser.typeHolder()
        val structType = typeHolder.getVarTypeOrNull("a") ?: error("Cannot find struct type")
        assertEquals("struct A {int a;union union.1 {int b;char c;}}", structType.toString())
        val ty = structType.typeDesc.cType() as CStructType
        assertEquals(0, ty.fieldByName("a").index)
        assertEquals(1, ty.fieldByName("b").index)
        assertEquals(1, ty.fieldByName("c").index)
    }

    @Test
    fun testAnonMember2() {
        val input = """
            |struct C {
            |    union {
            |        struct {
            |            int i, j;
            |        };
            |        struct {
            |            long k, l;
            |        } w;
            |    };
            |    int m;
            |} c;
        """.trimMargin()
        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        parser.translation_unit()
        val typeHolder = parser.typeHolder()
        val structType = typeHolder.getVarTypeOrNull("c") ?: error("Cannot find struct type")
        assertEquals("struct C {union union.5 {struct struct.3 {int i;int j;}struct struct.4 {long k;long l;} w;}int m;}", structType.toString())
        val ty = structType.typeDesc.cType() as CStructType
        assertEquals(0, ty.fieldByName("i").index)
        assertEquals(1, ty.fieldByName("j").index)
        assertEquals(0, ty.fieldByName("w").index)
    }

    @Test
    fun testInitializerList0() {
        val input = """
          int a[] = {1, 2, 3};
        """.trimIndent()
        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        parser.translation_unit()
        val typeHolder = parser.typeHolder()
        assertEquals("[3]int", typeHolder["a"].toString())
    }

    @Test
    fun testInitializerList1() {
        val input = """
          int a[][3] = {{1, 2, 3}, {4, 5, 6}};
        """.trimIndent()
        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        parser.translation_unit()
        val typeHolder = parser.typeHolder()
        assertEquals("[2][3]int", typeHolder["a"].toString())
    }

    @Test
    fun testInitializerList2() {
        val input = """
          int a[2][3] = {{1, 2, 3}, {4, 5, 6}};
        """.trimIndent()
        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        parser.translation_unit()
        val typeHolder = parser.typeHolder()
        assertEquals("[2][3]int", typeHolder["a"].toString())
    }

    @Test
    fun testInitializerList3() {
        val input = """
            typedef struct point { int x; int y; } Point;
            
            Point a[] = {{1, 2}, {3, 4}};
        """.trimIndent()
        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        parser.translation_unit()
        val typeHolder = parser.typeHolder()
        assertEquals("[2]struct point {int x;int y;}", typeHolder["a"].toString())
    }

    @Test
    fun testInitializerList4() {
        val input = """
            typedef struct Vec { int point[3]; } Vect3;
            
            Vect3 a[] = {{{1, 2, 3}}, {{4, 5, 6}}, {{7, 8, 9}}, {{10, 11, 12}}};
        """.trimIndent()
        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        parser.translation_unit()
        val typeHolder = parser.typeHolder()
        assertEquals("[4]struct Vec {[3]int point;}", typeHolder["a"].toString())
    }

    @Test
    fun testInitializerList5() {
        val input = """
          int a[] = {1, 2, 3};
        """.trimIndent()
        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        parser.translation_unit()
        val typeHolder = parser.typeHolder()
        assertEquals("[3]int", typeHolder["a"].toString())
    }

    @Test
    fun testUncompletedArray() {
        val input = """
            typedef struct Array_ { int len; int arr[]; } Array;
            
            Array arr;
        """.trimIndent()
        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        parser.translation_unit()
        val typeHolder = parser.typeHolder()
        assertEquals("struct Array_ {int len;[]int arr;}", typeHolder["arr"].toString())
    }

    @Test
    fun testTypedefValue() {
        val input = """
            typedef unsigned long A;
        """.trimIndent()
        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        parser.translation_unit()
        val typeHolder = parser.typeHolder()
        assertEquals("unsigned long", typeHolder.getTypedef("A").cType().toString())
    }

    @Test
    fun testStaticStorageClass() {
        val input = """
            static int a = 10;
        """.trimIndent()
        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        parser.translation_unit()
        val typeHolder = parser.typeHolder()
        assertEquals(StorageClass.STATIC, typeHolder["a"].storageClass)
    }

    @Test
    fun testExternStorageClass1() {
        val input = """
            extern int a;
        """.trimIndent()
        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        parser.translation_unit()
        val typeHolder = parser.typeHolder()
        assertEquals(StorageClass.EXTERN, typeHolder["a"].storageClass)
    }

    @Test
    fun testExternStorageClass2() {
        val input = """
            extern int a = 10, b = 20;
        """.trimIndent()
        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        parser.translation_unit()
        val typeHolder = parser.typeHolder()
        assertEquals(StorageClass.EXTERN, typeHolder["a"].storageClass)
    }

    @Test
    fun testStaticStorageClass1() {
        val input = """
            static int a = 10, *b;
        """.trimIndent()
        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        val declaration = parser.declaration()!!
        val vars = declaration.declareVars(parser.typeHolder())
        val a = vars.find { it.name == "a" }!!
        assertEquals(StorageClass.STATIC, a.storageClass)
    }

    @Test
    fun testArrayOfPointers() {
        val input = """
            const char ptr0[] = "b";
            const char ptr[] = "a";
            static char* table[][2] = {{ ptr0, ptr }};
        """.trimIndent()

        val tokens = apply(input)
        val parser = CProgramParser.build(tokens)

        parser.translation_unit()
        val typeHolder = parser.typeHolder()
        println(typeHolder)
    }
}