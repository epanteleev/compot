package asm
import asm.x64.*
import kotlin.test.Test
import kotlin.test.assertEquals

class AsmTest {
    @Test
    fun test1() {
        val asm = Assembler()
        val fn = asm.mkFunction("main")
        fn.push(Rbp(8))
        fn.mov(Rsp(8), Rbp(8))
        fn.sub(Imm(16, 8), Rsp(8))
        fn.mov(Rdi(8), Mem.mem(Rbp(8),-8, 8))

        fn.mov(Imm(0, 8), Rax(8))
        fn.mov(Rbp(8), Rsp(8))
        fn.ret()

        val expected =
        """
        .global main
        
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