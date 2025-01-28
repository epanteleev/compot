package ssa.asm

import asm.x64.*
import asm.x64.GPRegister.*
import ir.platform.x64.CompilationUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class AsmTest {
    @Test
    fun test1() {
        val asm = CompilationUnit()
        asm.global("main")
        asm.section(TextSection)
        val fn = asm.function("main")
        fn.push(8, rbp)
        fn.copy(8, rsp, rbp)
        fn.sub(8, Imm32.of(16), rsp)
        fn.mov(8, rdi, Address.from(rbp,-8))

        fn.copy(8, Imm32.of(0), rax)
        fn.copy(8, rbp, rsp)
        fn.ret()

        val expected = """
        .global main
        .text
        main:
            pushq %rbp
            movq %rsp, %rbp
            subq $16, %rsp
            movq %rdi, -8(%rbp)
            xorq %rax, %rax
            movq %rbp, %rsp
            ret
        
        """.trimIndent()
        assertEquals(expected, asm.toString())
    }
}