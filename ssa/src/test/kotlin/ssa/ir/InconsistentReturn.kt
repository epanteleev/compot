package ssa.ir

import ir.I32Value
import ir.types.Type
import kotlin.test.Test
import org.junit.jupiter.api.assertThrows
import ir.module.builder.impl.ModuleBuilder
import ir.pass.ana.ValidateSSAErrorException


class InconsistentReturn {
    @Test
    fun testInconsistentReturn() {
        val builder = ModuleBuilder.create()
        builder.createFunction("main", Type.I64, arrayListOf()).apply {
            ret(I32Value(0))
        }

        assertThrows<ValidateSSAErrorException>{ builder.build() }
    }

    @Test
    fun testReturnVoid() {
        val builder = ModuleBuilder.create()
        builder.createFunction("main", Type.I32, arrayListOf()).apply {
            retVoid()
        }

        assertThrows<ValidateSSAErrorException>{ builder.build() }
    }
}