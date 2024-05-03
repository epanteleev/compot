package ssa.collections

import asm.x64.GPRegister
import asm.x64.GPRegister.*
import common.intSetOf
import kotlin.test.Test
import kotlin.test.assertEquals

class IntSetTest {
    @Test
    fun regsetTest() {
        val gpRegSet = intSetOf(GPRegister.NUMBER_OF_GP_REGISTERS, rdi, rsi, rdx, rcx, r8, r9) { it.encoding() }
        assertEquals(6, gpRegSet.size)
    }
}