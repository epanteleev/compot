package shlang

import common.CommonCTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class LLVMCTests: CommonCTest() {
    @Test
    fun testPrintChar() {
        val result = runCTest("shlang/llvm-c-tests/2002-04-17-PrintfChar", listOf(), options())
        assertEquals("'c' 'e'\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testArgumentTest() {
        val result = runCTest("shlang/llvm-c-tests/2002-05-02-ArgumentTest", listOf(), options())
        assertEquals("12, 1.245000, 120, 123456677890, -10, 4500000000000000.000000\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testCast() {
        val result = runCTest("shlang/llvm-c-tests/2002-05-02-CastTest", listOf(), options())
        val expected = """ubc0 = 'd'	   [0x64]
        |ubs0 = 255	   [0xff]
        |ubs1 = 0	   [0x0]
        |bs0  = -1	   [0xffffffff]
        |bs1  = 0	   [0x0]
        |c1   = 'd'	   [0x64]
        |s1   = -769	   [0xfffffcff]
        |uc2  = 'd'	   [0x64]
        |us2  = 64767	   [0xfcff]
        |ic3  = 'd'	   [0x64]
        |is3  = -769	   [0xfffffcff]
        |is4  = 256	   [0x100]
        |is5  = 0	   [0x0]
        |uic4 = 'd'	   [0x64]
        |uis4 = 4294966527  [0xfffffcff]
        |
        """.trimMargin()
        assertEquals(expected, result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testCast1() {
        val result = runCTest("shlang/llvm-c-tests/2002-05-02-CastTest1", listOf(), options())
        assertEquals("bs0  = -1 255\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testCast2() {
        val result = runCTest("shlang/llvm-c-tests/2002-05-02-CastTest2", listOf(), options())
        assertEquals("us2  = 64767\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testCast3() {
        val result = runCTest("shlang/llvm-c-tests/2002-05-02-CastTest3", listOf(), options())
        assertEquals("s1   = -769\nus2  = 64767\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testManyArgs() {
        val result = runCTest("shlang/llvm-c-tests/2002-05-02-ManyArguments", listOf(), options())
        val expected = """
            |12, 2, 123.234000, 1231.123169, -12
            |23, 123456, 0, 1234567, 123124124124
            |""".trimMargin()

        assertEquals(expected, result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testNot() {
        val result = runCTest("shlang/llvm-c-tests/2002-05-03-NotTest", listOf(), options())
        val expected = """
            Bitwise Not: -2 -3 2 -6
            Boolean Not: 0 1 0 1 0 1
            
            """.trimIndent()
        assertEquals(expected, result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testDiv() {
        val result = runCTest("shlang/llvm-c-tests/2002-05-19-DivTest", listOf(), options())
        val expected = """
            |-1048544
            |-65534
            |-3
            |0
            |-1048543
            |-65533
            |-3
            |0
            |4
            |-127
            |5
            |-127
            |""".trimMargin()
        assertEquals(expected, result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testCast_08_02() {
        val result = runCTest("shlang/llvm-c-tests/2002-08-02-CastTest", listOf(), options())
        assertEquals("64\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testCast_08_02_2() {
        val result = runCTest("shlang/llvm-c-tests/2002-08-02-CastTest2", listOf(), options())
        val expected = """
            s1   = -769
            us2  = 64767
            
        """.trimIndent()
        assertEquals(expected, result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testCodegenBug() {
        val result = runCTest("shlang/llvm-c-tests/2002-08-19-CodegenBug", listOf(), options())
        assertEquals("SUCCESS\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    @Ignore
    fun testArrayResolution() {
        val result = runCTest("shlang/llvm-c-tests/2002-10-09-ArrayResolution", listOf(), options())
        assertEquals("0\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    @Ignore
    fun testStructureArgs() {
        val result = runCTest("shlang/llvm-c-tests/2002-10-12-StructureArgs", listOf(), options())
        assertEquals("0.500000, 1.200000, -123.010000, 0.333333\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    @Ignore
    fun testStructureArgsSimple() {
        val result = runCTest("shlang/llvm-c-tests/2002-10-12-StructureArgsSimple", listOf(), options())
        assertEquals("0.500000, 1.200000\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testBadLoad() {
        val result = runCTest("shlang/llvm-c-tests/2002-10-13-BadLoad", listOf(), options())
        assertEquals("65536\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testMishaTest() {
        val result = runCTest("shlang/llvm-c-tests/2002-12-13-MishaTest", listOf(), options())
        assertEquals("Sum is 1\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    @Ignore
    fun testSwitch() {
        val result = runCTest("shlang/llvm-c-tests/2003-04-22-Switch", listOf(), options())
        assertEquals("A\nB\nA\nA\nD\nD\nD\nB\nC\nA\nB\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    @Ignore
    fun testDependentPhi() {
        val result = runCTest("shlang/llvm-c-tests/2003-05-02-DependentPHI", listOf(), options())
        val expected = """0 -1
        1 0
        2 1
        3 2
        4 3
        5 4""".trimIndent()
        assertEquals(expected, result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    @Ignore
    fun testVarArgs() {
        val result = runCTest("shlang/llvm-c-tests/2003-05-07-VarArgs", listOf(), options())
        val expected = """string abc
        string def
        int -123
        char a
        int 123
        int 6
        int 7
        int 8
        int 9
        string 10 args done!
        double 1.000000
        double 2.000000
        int 32764
        long long 12345677823423
        DWord { 18, a }
        QuadWord { 19, 20.000000 }
        LargeS { 21, 22.000000, 0x1, 23 }
        """.trimIndent()
        assertEquals(expected, result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testMinIntProblem() {
        val result = runCTest("shlang/llvm-c-tests/2003-05-12-MinIntProblem", listOf(), options())
        assertEquals("success\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testAtExit() {
        val result = runCTest("shlang/llvm-c-tests/2003-05-14-AtExit", listOf(), options())
        assertEquals("in main\nExiting!\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testShorts() {
        val result = runCTest("shlang/llvm-c-tests/2003-05-26-Shorts", listOf(), options())
        val expected = readExpectedOutput("shlang/llvm-c-tests/2003-05-26-Shorts.reference_output")
        assertEquals(expected, result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testCastToBool() {
        val result = runCTest("shlang/llvm-c-tests/2003-05-31-CastToBool", listOf(), options())
        val expected = """
            0 0
            123 1
            0 0
            1234 1
            0 0
            1234 1
            0 0
            123121231231231 1
            1230098424783699968 1
            69920 1
            y = 2, (y == 2 || y == 0) == 1
            y = 2, (y > 2 || y < 5) == 0
            y = 2, (y ^ 2 ^ ~y) == 1
            
        """.trimIndent()
        assertEquals(expected, result.output)
        assertEquals(0, result.exitCode)
    }
}

class LLVMCTestsO0: LLVMCTests() {
    override fun options(): List<String> = listOf()
}

class LLVMCTestsO1: LLVMCTests() {
    override fun options(): List<String> = listOf("-O1")
}