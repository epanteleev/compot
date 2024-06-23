package ssa.ir

import ir.F32Value
import ir.module.FunctionPrototype
import ir.I32Value
import ir.NullValue
import ir.U32Value
import ir.types.Type
import kotlin.test.Test
import ir.module.builder.impl.ModuleBuilder
import ir.pass.ana.ValidateSSAErrorException
import ir.pass.ana.VerifySSA
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue


class InconsistentCFG {
    @Test
    fun testInconsistentReturn() {
        val builder = ModuleBuilder.create()
        builder.createFunction("main", Type.I64, arrayListOf()).apply {
            ret(I32Value(0))
        }

        val throwable = assertFails { builder.build() }
        assertTrue { throwable is ValidateSSAErrorException }
    }

    @Test
    fun testReturnVoid() {
        val builder = ModuleBuilder.create()
        builder.createFunction("main", Type.I32, arrayListOf()).apply {
            retVoid()
        }

        val throwable = assertFails { builder.build() }
        assertTrue { throwable is ValidateSSAErrorException }
    }

    @Test
    fun testCallF32() {
        val builder = ModuleBuilder.create()
        builder.createExternFunction("calc", Type.F32, arrayListOf(Type.F32))
        val invalidPrototype = FunctionPrototype("calc", Type.I32, arrayListOf(Type.F32))

        builder.createFunction("main", Type.I32, arrayListOf()).apply {
            val cont = createLabel()
            call(invalidPrototype, arrayListOf(F32Value(0.0F)), cont)
            switchLabel(cont)
            ret(I32Value(0))
        }

        val throwable = assertFails { builder.build() }
        assertTrue { throwable is ValidateSSAErrorException }
    }

    @Test
    fun testMultiplyTerminateInstructions() {
        val builder = ModuleBuilder.create()

        builder.createFunction("main", Type.I32, arrayListOf()).apply {
            val header = currentLabel().let {
                val header = createLabel()
                branch(header)
                header
            }

            val label = header.let {
                switchLabel(header)
                val label = createLabel()
                branch(label)
                ret(I32Value(0))
                label
            }

            label.apply {
                switchLabel(label)
                branch(header)
            }
        }

        val throwable = assertFails { builder.build() }
        assertTrue { throwable is ValidateSSAErrorException }
    }

    @Test
    fun testMultiplyProjections() {
        val builder = ModuleBuilder.create()

        builder.createFunction("main", Type.U32, arrayListOf()).apply {
            val tuple = tupleDiv(U32Value(100), U32Value(20))

            proj(tuple, 0)
            val proj1 = proj(tuple, 0)
            ret(proj1)
        }

        val throwable = assertFails { builder.build() }
        assertTrue { throwable is ValidateSSAErrorException }
    }
}