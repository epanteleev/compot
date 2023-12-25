package asm

import asm.x64.*
import asm.x64.GPRegister.*
import kotlin.test.Test
import kotlin.test.assertEquals

class AsmTest {
    @Test
    fun test1() {
        val asm = Assembler()
        val fn = asm.mkFunction("main")
        fn.push(8, rbp)
        fn.mov(8, rsp, rbp)
        fn.sub(8, ImmInt(16), rsp)
        fn.mov(8, rdi, Address.mem(rbp,-8))

        fn.mov(8, ImmInt(0), rax)
        fn.mov(8, rbp, rsp)
        fn.ret()

        val expected =
        """
        .global main
        
        .text
        main:
            pushq %rbp
            movq %rsp, %rbp
            subq $16, %rsp
            movq %rdi, -8(%rbp)
            movq $0, %rax
            movq %rbp, %rsp
            ret
        """.trimIndent()
        assertEquals(expected, asm.toString())
    }
}