package ssa.ir

import ir.instruction.IntPredicate
import ir.value.constant.F32Value
import ir.module.FunctionPrototype
import ir.types.Type
import kotlin.test.Test
import ir.module.builder.impl.ModuleBuilder
import ir.pass.analysis.ValidateSSAErrorException
import ir.value.constant.*
import kotlin.collections.hashSetOf
import kotlin.test.assertFails
import kotlin.test.assertTrue


class InconsistentCFG {
    @Test
    fun testInconsistentReturn() {
        val builder = ModuleBuilder.create()
        builder.createFunction("main", Type.I64, arrayListOf()).apply {
            ret(Type.I32, arrayOf(I32Value(0)))
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
        val invalidPrototype = FunctionPrototype("calc", Type.I32, arrayListOf(Type.F32), hashSetOf())

        builder.createFunction("main", Type.I32, arrayListOf()).apply {
            val cont = createLabel()
            call(invalidPrototype, arrayListOf(F32Value(0.0F)), cont)
            switchLabel(cont)
            ret(Type.I32, arrayOf(I32Value(0)))
        }

        val throwable = assertFails { builder.build() }
        assertTrue { throwable is ValidateSSAErrorException }
    }

    @Test
    fun testMultiplyTerminateInstructions() {
        val builder = ModuleBuilder.create()

        val throwable = assertFails {
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
                    ret(Type.I32, arrayOf(I32Value(0)))
                    label
                }

                label.apply {
                    switchLabel(label)
                    branch(header)
                }
            }
            builder.build()
        }
        assertTrue { throwable is IllegalStateException }
    }

    @Test
    fun testMultiplyProjections() {
        val builder = ModuleBuilder.create()

        builder.createFunction("main", Type.U32, arrayListOf()).apply {
            val tuple = tupleDiv(U32Value(100), U32Value(20))

            proj(tuple, 0)
            val proj1 = proj(tuple, 0)
            ret(Type.U32, arrayOf(proj1))
        }

        val throwable = assertFails { builder.build() }
        assertTrue { throwable is ValidateSSAErrorException }
    }

    @Test
    fun testInconsistentCondBranch() {
        val builder = ModuleBuilder.create()

        builder.createFunction("main", Type.I32, arrayListOf(Type.I32)).apply {
            val arg = argument(0)
            val cmp = icmp(I32Value(0), IntPredicate.Eq, arg)
            val add = add(I32Value(0), arg)
            val cont = createLabel()
            val then = createLabel()
            branchCond(cmp, then, cont)
            switchLabel(then).let {
                branch(cont)
            }
            switchLabel(cont).let {
                ret(Type.I32, arrayOf(add))
            }
        }

        val throwable = assertFails { builder.build() }
        assertTrue { throwable is ValidateSSAErrorException }
    }

    @Test
    fun testInconsistentFlagToInt() {
        val builder = ModuleBuilder.create()

        builder.createFunction("main", Type.I32, arrayListOf(Type.I32)).apply {
            val arg = argument(0)
            val cmp = icmp(I32Value(0), IntPredicate.Eq, arg)
            val add = add(I32Value(0), arg)
            val i = flag2int(cmp, Type.I32)
            ret(Type.I32, arrayOf(i))
        }

        val throwable = assertFails { builder.build() }
        assertTrue { throwable is ValidateSSAErrorException }
    }

    @Test
    fun testInconsistentSelect() {
        val builder = ModuleBuilder.create()

        builder.createFunction("main", Type.I32, arrayListOf(Type.I32)).apply {
            val arg = argument(0)
            val cmp = icmp(I32Value(0), IntPredicate.Eq, arg)
            val add = add(I32Value(0), arg)
            val i = select(cmp, Type.I32, I32Value(0), add)
            ret(Type.I32, arrayOf(i))
        }

        val throwable = assertFails { builder.build() }
        assertTrue { throwable is ValidateSSAErrorException }
    }
}