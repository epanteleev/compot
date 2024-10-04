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
    fun testArrayResolution() {
        val result = runCTest("shlang/llvm-c-tests/2002-10-09-ArrayResolution", listOf(), options())
        assertEquals("0\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testStructureArgs() {
        val result = runCTest("shlang/llvm-c-tests/2002-10-12-StructureArgs", listOf(), options())
        assertEquals("0.500000, 1.200000, -123.010000, 0.333333\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
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

    @Test
    fun testLongShift() {
        val result = runCTest("shlang/llvm-c-tests/2003-05-31-LongShifts", listOf(), options())
        val expected = readExpectedOutput("shlang/llvm-c-tests/2003-05-31-LongShifts.reference_output")
        assertEquals(expected, result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testIntOverflow() {
        val result = runCTest("shlang/llvm-c-tests/2003-07-06-IntOverflow", listOf(), options())
        val expected = """
            compare after overflow is TRUE
            divide after overflow = -170 (0xffffff56)
            divide negative value by power-of-2 = -16 (0xfffffff0)
            subtract after overflow = 2134900731 (0x7f3ffffb)
            
        """.trimIndent()
        assertEquals(expected, result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testBitOps() {
        val result = runCTest("shlang/llvm-c-tests/2003-07-08-BitOpsTest", listOf(), options())
        val expected = "-15 -1 0 -3 12\n"
        assertEquals(expected, result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testLoadShorts() {
        val result = runCTest("shlang/llvm-c-tests/2003-07-09-LoadShorts", listOf(), options())
        val expected = readExpectedOutput("shlang/llvm-c-tests/2003-07-09-LoadShorts.reference_output")
        assertEquals(expected, result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    @Ignore
    fun testSignedArgs() {
        val result = runCTest("shlang/llvm-c-tests/2003-07-09-SignedArgs", listOf(), options())
        val expected = """getShort():	1 1 1 1 1 1
        getShort():	-128 116 116 -3852 -31232 -1708916736
        getUnknown():	-128 116 116 -3852 -31232 30556 -1708916736
        -1708921160
        """.trimIndent()
        assertEquals(expected, result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testSignConversion() {
        val result = runCTest("shlang/llvm-c-tests/2003-07-10-SignConversions", listOf(), options())
        val expected = """
            |-128 128 --> unsigned: us = 65408, us2 = 128
            |-128 128 -->   signed:  s = -128,  s2 = 128
            |-128 128 --> unsigned: uc = 128, uc2 = 128
            |-128 128 -->   signed: sc = -128, sc2 = -128
            |
        """.trimMargin()
        assertEquals(expected, result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testCastFPToUint() {
        val result = runCTest("shlang/llvm-c-tests/2003-08-05-CastFPToUint", listOf(), options())
        val expected = """
            |DC = 240.000000, DS = 65520.000000, DI = 4294967280.000000
            |uc = 240, us = 65520, ui = 4294967280
            |""".trimMargin()
        assertEquals(expected, result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    @Ignore
    fun testVaListArg() {
        val result = runCTest("shlang/llvm-c-tests/2003-08-11-VaListArg", listOf(), options())
        val expected = readExpectedOutput("shlang/llvm-c-tests/2003-08-11-VaListArg.reference_output")
        assertEquals(expected, result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testFoldBug() {
        val result = runCTest("shlang/llvm-c-tests/2003-08-20-FoldBug", listOf(), options())
        assertEquals("All ok\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testBitFieldTest() {
        val result = runCTest("shlang/llvm-c-tests/2003-09-18-BitFieldTest", listOf(), options())
        assertEquals("0 1\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testSwitch2() {
        val result = runCTest("shlang/llvm-c-tests/2003-10-13-SwitchTest", listOf(), options())
        assertEquals("GOOD\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    @Ignore
    fun testScalarReplBug() {
        val result = runCTest("shlang/llvm-c-tests/2003-10-29-ScalarReplBug", listOf(), options())
        assertEquals("0\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testNegativeZero() {
        val result = runCTest("shlang/llvm-c-tests/2004-02-02-NegativeZero", listOf(), options())
        val expected = """
        |-0.000000 -0.000000
        |0.000000 0.000000
        |negzero = -0.000000  poszero = 0.000000
        |
        """.trimMargin()
        assertEquals(expected, result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testStaticBitFieldInit() {
        val result = runCTest("shlang/llvm-c-tests/2004-06-20-StaticBitfieldInit", listOf(), options())
        assertEquals("1 5 1\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testGlobalBoolLayout() {
        val result = runCTest("shlang/llvm-c-tests/2004-11-28-GlobalBoolLayout", listOf(), options())
        assertEquals("1 1 4\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testInt64ToFP() {
        val result = runCTest("shlang/llvm-c-tests/2005-05-12-Int64ToFP", listOf(), options())
        val expected = readExpectedOutput("shlang/llvm-c-tests/2005-05-12-Int64ToFP.reference_output")
        assertEquals(expected, result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testSDivTwo() {
        val result = runCTest("shlang/llvm-c-tests/2005-05-13-SDivTwo", listOf(), options())
        val expected = readExpectedOutput("shlang/llvm-c-tests/2005-05-13-SDivTwo.reference_output")
        assertEquals(expected, result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    @Ignore
    fun testBitField_ABI() {
        val result = runCTest("shlang/llvm-c-tests/2005-07-15-Bitfield-ABI", listOf(), options())
        assertEquals("fffffc3f\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testIntTOFP() {
        val result = runCTest("shlang/llvm-c-tests/2005-07-17-INT-To-FP", listOf(), options())
        val expected = readExpectedOutput("shlang/llvm-c-tests/2005-07-17-INT-To-FP.reference_output")
        assertEquals(expected, result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testLongSwitch() {
        val result = runCTest("shlang/llvm-c-tests/2005-11-29-LongSwitch", listOf(), options())
        assertEquals("foo = 0\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    @Ignore
    fun testUnionInit() {
        val result = runCTest("shlang/llvm-c-tests/2006-01-23-UnionInit", listOf(), options())
        val expect = """
            |PR156: swapper
            |PR295/PR568: 1888, 256
            |PR574: 61172160, 4, 22, 2
            |PR162: 1, 2, 513
            |PR650: relname, 1852597618
            |PR199: 5, 1, 2, 3
            |PR199: 5, 1, 2, 3
            |PR431: 0, 1, 2, 3
            |PR654: 0, '   xyzkasjdlf     '
            |PR323: 3, 'foo'
            |returning raw_lock
            |PR627: 0
            |PR684: 1, 2, 0 1
            |rdar://6828787: 23122, -12312731, -312
        """.trimMargin()
        assertEquals(expect, result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testSimpleIndirectCall() {
        val result = runCTest("shlang/llvm-c-tests/2006-01-29-SimpleIndirectCall", listOf(), options())
        assertEquals("Goodbye, world!\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testDivRem() {
        val result = runCTest("shlang/llvm-c-tests/2006-02-04-DivRem", listOf(), options())
        val expected = readExpectedOutput("shlang/llvm-c-tests/2006-02-04-DivRem.reference_output")
        assertEquals(expected, result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testFloat_Varg() {
        val result = runCTest("shlang/llvm-c-tests/2006-12-01-float_varg", listOf(), options())
        assertEquals("foo 1.230000 12312.100000 3.100000 13.100000\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testCompare64BitConstant() {
        val result = runCTest("shlang/llvm-c-tests/2006-12-07-Compare64BitConstant", listOf(), options())
        assertEquals("Works.\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testLoadConstants() {
        val result = runCTest("shlang/llvm-c-tests/2006-12-11-LoadConstants", listOf(), options())
        val expected = readExpectedOutput("shlang/llvm-c-tests/2006-12-11-LoadConstants.reference_output")
        assertEquals(expected, result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    fun testKNR_Args() {
        val result = runCTest("shlang/llvm-c-tests/2007-01-04-KNR-Args", listOf(), options())
        val expected = """
            |a 4.000000 1 5.000000 2 4.000000 3 5.000000
            |a 4.000000 1 5.000000 2 4.000000 3 5.000000
            |
        """.trimMargin()
        assertEquals(expected, result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    @Ignore
    fun testVaCopy() {
        val result = runCTest("shlang/llvm-c-tests/2007-03-02-VaCopy", listOf(), options())
        assertEquals("string abc\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    @Ignore
    fun testBitfield() {
        val result = runCTest("shlang/llvm-c-tests/2007-04-10-BitfieldTest", listOf(), options())
        assertEquals("p = 0x24\n", result.output)
        assertEquals(0, result.exitCode)
    }

    @Test
    @Ignore
    fun testLoopBug() {
        val result = runCTest("shlang/llvm-c-tests/2008-04-18-LoopBug", listOf(), options())
        val expected = """0 5 5 6 7
        |1 5 6 6 7
        |2 5 6 7 7
        |3 5 6 7 8
        |4 5 6 7 8
        |-1 5 6 7 8
        """.trimMargin()
        assertEquals("0\n", result.output)
        assertEquals(0, result.exitCode)
    }
}

class LLVMCTestsO0: LLVMCTests() {
    override fun options(): List<String> = listOf()
}

class LLVMCTestsO1: LLVMCTests() {
    override fun options(): List<String> = listOf("-O1")
}