
import types.*
import parser.CProgramParser
import parser.LineAgnosticAstPrinter
import parser.nodes.*
import tokenizer.CTokenizer
import typedesc.StorageClass
import typedesc.TypeHolder
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
        assertEquals(INT, type)
    }

    @Test
    fun testFloat1() {
        val tokens = CTokenizer.apply("2.0 + 3.0")
        val parser = CProgramParser.build(tokens)

        val expr = parser.assignment_expression() as Expression
        val typeResolver = TypeHolder.default()
        val type = expr.resolveType(typeResolver)
        assertEquals(DOUBLE, type)
    }

    @Test
    fun testDeclarationSpecifiers1() {
        val tokens = CTokenizer.apply("volatile int restrict")
        val parser = CProgramParser.build(tokens)

        val expr = parser.declaration_specifiers() as DeclarationSpecifier
        val typeResolver = TypeHolder.default()
        assertEquals("volatile restrict int", expr.specifyType(typeResolver, listOf()).toString())
    }

    @Test
    fun testDeclarationSpecifiers2() {
        val tokens = CTokenizer.apply("volatile float restrict")
        val parser = CProgramParser.build(tokens)

        val expr = parser.declaration_specifiers() as DeclarationSpecifier
        val typeResolver = TypeHolder.default()
        assertEquals("volatile restrict float", expr.specifyType(typeResolver, listOf()).toString())
    }

    @Test
    fun testDeclarationSpecifiers3() {
        val tokens = CTokenizer.apply("volatile struct point")
        val parser = CProgramParser.build(tokens)

        val expr = parser.declaration_specifiers() as DeclarationSpecifier
        val typeResolver = parser.typeHolder()
        assertEquals("volatile struct point", expr.specifyType(typeResolver, listOf()).toString())
    }

    @Test
    fun testDecl() {
        val tokens = CTokenizer.apply("int a = 3 + 6;")
        val parser = CProgramParser.build(tokens)

        val expr = parser.declaration() as Declaration
        println(expr)
        val typeResolver = TypeHolder.default()
        expr.specifyType(typeResolver, listOf())
        assertEquals(INT, typeResolver["a"].typeDesc.cType())
    }

    @Test
    fun testDecl2() {
        val tokens = CTokenizer.apply("int a = 3 + 6, v = 90;")
        val parser = CProgramParser.build(tokens)

        val expr = parser.declaration() as Declaration
        println(expr)
        val typeResolver = TypeHolder.default()
        expr.specifyType(typeResolver, listOf())
        assertEquals(INT, typeResolver["a"].typeDesc.cType())
        assertEquals(INT, typeResolver["v"].typeDesc.cType())
    }

    @Test
    fun testDecl3() {
        val tokens = CTokenizer.apply("int a = 3 + 6, v = 90, *p = &v;")
        val parser = CProgramParser.build(tokens)

        val expr = parser.declaration() as Declaration
        println(expr)
        val typeResolver = TypeHolder.default()
        expr.specifyType(typeResolver, listOf())

        assertTrue { typeResolver.containsVar("a") }
        assertTrue { typeResolver.containsVar("v") }
        assertTrue { typeResolver.containsVar("p") }
        assertEquals(INT, typeResolver["a"].typeDesc.cType())
        assertEquals(INT, typeResolver["v"].typeDesc.cType())
        assertEquals(CPointer(INT), typeResolver["p"].typeDesc.cType())
    }

    @Test
    fun testDecl4() {
        val tokens = CTokenizer.apply("struct point* a = 0;")
        val parser = CProgramParser.build(tokens)

        val expr = parser.declaration() as Declaration
        println(expr)
        val typeResolver = TypeHolder.default()
        expr.specifyType(typeResolver, listOf())
        assertEquals("struct point*", typeResolver["a"].toString())
    }

    @Test
    fun testDecl5() {
        val tokens = CTokenizer.apply("struct point* a = 0, *b = 0;")
        val parser = CProgramParser.build(tokens)

        val expr = parser.declaration() as Declaration
        println(expr)
        val typeResolver = TypeHolder.default()
        expr.specifyType(typeResolver, listOf())
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
        assertEquals(INT, typeResolver["a"].typeDesc.cType())
        assertEquals(INT, typeResolver["b"].typeDesc.cType())
    }

    @Test
    fun testDecl7() {
        val tokens = CTokenizer.apply("int add(void) { }")
        val parser = CProgramParser.build(tokens)
        val expr = parser.function_definition() as FunctionNode
        val typeResolver = TypeHolder.default()
        val fnType = expr.resolveType(typeResolver)

        assertEquals("int add()", fnType.toString())
    }

    @Test
    fun testDecl8() {
        val tokens = CTokenizer.apply("int add(int (a)(int, int), int b) { }")
        val parser = CProgramParser.build(tokens)
        val expr = parser.function_definition() as FunctionNode
        val typeResolver = TypeHolder.default()
        val fnType = expr.resolveType(typeResolver)

        assertEquals("int add(int(int, int)*, int)", fnType.toString())
        assertEquals("int(int, int)*", typeResolver["a"].toString())
        assertEquals(INT, typeResolver["b"].typeDesc.cType())
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
        val typeResolver = TypeHolder.default()
        expr.specifyType(typeResolver, listOf())

        assertEquals("int(int, int)*", typeResolver["add"].toString())
    }

    @Test
    fun testFunctionPointerDeclaration1() {
        val tokens = CTokenizer.apply("int (*add)(int, int);")
        val parser = CProgramParser.build(tokens)
        val expr = parser.declaration() as Declaration
        val typeResolver = TypeHolder.default()
        expr.specifyType(typeResolver, listOf())

        assertEquals("int(int, int)*", typeResolver["add"].toString())
    }

    @Test
    fun testFunctionPointerDeclaration2() {
        val tokens = CTokenizer.apply("int (*add)(void) = 0, val = 999, *v = 0;")
        val parser = CProgramParser.build(tokens)
        val expr = parser.declaration() as Declaration
        val typeResolver = TypeHolder.default()
        expr.specifyType(typeResolver, listOf())

        assertEquals("int()*", typeResolver["add"].toString())
        assertEquals("int", typeResolver["val"].toString())
        assertEquals("int*", typeResolver["v"].toString())
    }

    @Test
    fun testFunctionDeclarator() {
        val tokens = CTokenizer.apply("int b(int b), n(float f);")
        val parser = CProgramParser.build(tokens)
        parser.translation_unit()
        val typeResolver = parser.typeHolder()

        assertEquals("int b(int)", typeResolver.getFunctionType("b").toString())
        assertEquals("int n(float)", typeResolver.getFunctionType("n").toString())
    }

    @Test
    fun testStructDeclaration() {
        val tokens = CTokenizer.apply("struct point { int x; int y; };")
        val parser = CProgramParser.build(tokens)
        val expr = parser.declaration() as Declaration

        val typeResolver = TypeHolder.default()
        expr.specifyType(typeResolver, listOf())

        assertEquals("struct point {int x;int y;}", typeResolver.getStructType<CStructType>("point").toString())
    }

    @Test
    fun testStructDeclaration1() {
        val tokens = CTokenizer.apply("struct point;")
        val parser = CProgramParser.build(tokens)
        val expr = parser.declaration() as Declaration
        val typeResolver = parser.typeHolder()
        expr.specifyType(typeResolver, listOf())
        assertEquals("struct point", typeResolver.getStructType<CStructType>("point").toString())
    }

    @Test
    fun testArrayDeclaration() {
        val tokens = CTokenizer.apply("int a[10];")
        val parser = CProgramParser.build(tokens)
        val expr = parser.declaration() as Declaration

        val typeResolver = TypeHolder.default()
        expr.specifyType(typeResolver, listOf())

        val a = typeResolver["a"]
        assertEquals("[10]int", a.toString())
    }

    @Test
    fun testArrayDeclaration1() {
        val tokens = CTokenizer.apply("int a[5 + 5];")
        val parser = CProgramParser.build(tokens)
        val expr = parser.declaration() as Declaration

        val typeResolver = TypeHolder.default()
        expr.specifyType(typeResolver, listOf())

        val a = typeResolver["a"]
        assertEquals("[10]int", a.toString())
    }

    @Test
    fun testArrayDeclaration2() {
        val tokens = CTokenizer.apply("int a[10], b[20];")
        val parser = CProgramParser.build(tokens)
        val expr = parser.declaration() as Declaration

        val typeResolver = TypeHolder.default()
        expr.specifyType(typeResolver, listOf())

        val a = typeResolver["a"]
        val b = typeResolver["b"]
        assertEquals("[10]int", a.toString())
        assertEquals("[20]int", b.toString())
    }

    @Test
    fun testArrayDeclaration3() {
        val tokens = CTokenizer.apply("int a[10], b[20], *c = 0;")
        val parser = CProgramParser.build(tokens)
        val expr = parser.declaration() as Declaration

        val typeResolver = TypeHolder.default()
        expr.specifyType(typeResolver, listOf())

        val a = typeResolver["a"]
        val b = typeResolver["b"]
        val c = typeResolver["c"]
        assertEquals("[10]int", a.toString())
        assertEquals("[20]int", b.toString())
        assertEquals("int*", c.toString())
    }

    @Test
    fun testArrayDeclaration4() {
        val tokens = CTokenizer.apply("int a[10][30];")
        val parser = CProgramParser.build(tokens)
        val expr = parser.declaration() as Declaration

        val typeResolver = TypeHolder.default()
        expr.specifyType(typeResolver, listOf())

        val a = typeResolver["a"]
        assertEquals("[10][30]int", a.toString())
    }

    @Test
    fun testConstString() {
        val tokens = CTokenizer.apply("const char* a = \"hello\";")
        val parser = CProgramParser.build(tokens)
        val expr = parser.declaration() as Declaration
        val typeResolver = TypeHolder.default()
        expr.specifyType(typeResolver, listOf())

        val a = typeResolver["a"]
        assertEquals("const char*", a.toString())
    }

    @Test
    fun testUnsignedInt() {
        val tokens = CTokenizer.apply("unsigned int a = 10;")
        val parser = CProgramParser.build(tokens)
        val expr = parser.declaration() as Declaration
        val typeResolver = parser.typeHolder()
        expr.specifyType(typeResolver, listOf())

        val a = typeResolver["a"]
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
        val tokens = CTokenizer.apply(input)
        val parser = CProgramParser.build(tokens)

        val program = parser.translation_unit()
        println(program)
        assertEquals("typedef struct tnode TNODE; struct tnode {int count; TNODE *left, *right;} ; TNODE s, *sp;", LineAgnosticAstPrinter.print(program))

        val typeHolder = parser.typeHolder()
        assertEquals("struct tnode {int count;struct tnode* left;struct tnode* right;}", typeHolder.getStructType<CStructType>("tnode").toString())
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
        assertEquals("[2][3]int", parser.typeHolder().getTypedef("A").toString())
    }

    @Test
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
        assertEquals("struct s1 {int x;}", typeHolder.getStructType<CStructType>("s1").toString())
        assertEquals("struct s2 {int x;}", typeHolder.getStructType<CStructType>("s2").toString())
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
        val tokens = CTokenizer.apply(input)
        val parser = CProgramParser.build(tokens)

        parser.translation_unit()
        val typeHolder = parser.typeHolder()
        val unionType = typeHolder.getVarTypeOrNull("b") ?: error("Cannot find union type")
        assertEquals("union B {int a;struct struct.0 {int b;char c;}}", unionType.toString())
        val ty = unionType.typeDesc.cType() as CUnionType
        assertEquals(0, ty.fieldByIndexOrNull("a")?.index)
        assertEquals(0, ty.fieldByIndexOrNull("b")?.index)
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
        val tokens = CTokenizer.apply(input)
        val parser = CProgramParser.build(tokens)

        parser.translation_unit()
        val typeHolder = parser.typeHolder()
        val structType = typeHolder.getVarTypeOrNull("a") ?: error("Cannot find struct type")
        assertEquals("struct A {int a;union union.0 {int b;char c;}}", structType.toString())
        val ty = structType.typeDesc.cType() as CStructType
        assertEquals(0, ty.fieldByIndex("a").index)
        assertEquals(1, ty.fieldByIndex("b").index)
        assertEquals(1, ty.fieldByIndex("c").index)
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
        val tokens = CTokenizer.apply(input)
        val parser = CProgramParser.build(tokens)

        parser.translation_unit()
        val typeHolder = parser.typeHolder()
        val structType = typeHolder.getVarTypeOrNull("c") ?: error("Cannot find struct type")
        assertEquals("struct C {union union.2 {struct struct.0 {int i;int j;}struct struct.1 {long k;long l;} w;}int m;}", structType.toString())
        val ty = structType.typeDesc.cType() as CStructType
        val desc = ty.fieldByIndex("w")
        assertEquals(0, desc.index)
        val unionType = ty.fieldByIndexOrNull(0)!!.cType() as CUnionType
        assertEquals(0, unionType.fieldByIndex("i").index)
        assertEquals(1, unionType.fieldByIndex("j").index)
        assertEquals(0, unionType.fieldByIndex("w").index)
    }

    @Test
    fun testInitializerList0() {
        val input = """
          int a[] = {1, 2, 3};
        """.trimIndent()
        val tokens = CTokenizer.apply(input)
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
        val tokens = CTokenizer.apply(input)
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
        val tokens = CTokenizer.apply(input)
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
        val tokens = CTokenizer.apply(input)
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
        val tokens = CTokenizer.apply(input)
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
        val tokens = CTokenizer.apply(input)
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
        val tokens = CTokenizer.apply(input)
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
        val tokens = CTokenizer.apply(input)
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
        val tokens = CTokenizer.apply(input)
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
        val tokens = CTokenizer.apply(input)
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
        val tokens = CTokenizer.apply(input)
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
        val tokens = CTokenizer.apply(input)
        val parser = CProgramParser.build(tokens)

        parser.translation_unit()
        val typeHolder = parser.typeHolder()
        assertEquals(StorageClass.STATIC, typeHolder["a"].storageClass)
    }

    @Test
    fun testArrayOfPointers() {
        val input = """
            const char ptr0[] = "b";
            const char ptr[] = "a";
            static char* table[][2] = {{ ptr0, ptr }};
        """.trimIndent()

        val tokens = CTokenizer.apply(input)
        val parser = CProgramParser.build(tokens)

        parser.translation_unit()
        val typeHolder = parser.typeHolder()
        println(typeHolder)
    }
}