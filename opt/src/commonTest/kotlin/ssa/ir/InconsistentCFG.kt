package ssa.ir

import ir.instruction.IntPredicate
import ir.value.constant.F32Value
import ir.module.FunctionPrototype
import kotlin.test.Test
import ir.module.builder.impl.ModuleBuilder
import ir.pass.analysis.ValidateSSAErrorException
import ir.types.*
import ir.value.constant.*
import kotlin.collections.hashSetOf
import kotlin.test.assertFails
import kotlin.test.assertTrue


class InconsistentCFG {
    @Test
    fun testInconsistentReturn() {
        val builder = ModuleBuilder.create()
        builder.createFunction("main", I64Type, arrayListOf()).apply {
            ret(I32Type, arrayOf(I32Value.of(0)))
        }

        val throwable = assertFails { builder.build() }
        assertTrue { throwable is ValidateSSAErrorException }
    }

    @Test
    fun testReturnVoid() {
        val builder = ModuleBuilder.create()
        builder.createFunction("main", I32Type, arrayListOf()).apply {
            retVoid()
        }

        val throwable = assertFails { builder.build() }
        assertTrue { throwable is ValidateSSAErrorException }
    }

    @Test
    fun testCallF32() {
        val builder = ModuleBuilder.create()
        builder.createExternFunction("calc", F32Type, arrayListOf(F32Type), setOf())
        val invalidPrototype = FunctionPrototype("calc", I32Type, arrayListOf(F32Type), hashSetOf())

        builder.createFunction("main", I32Type, arrayListOf()).apply {
            val cont = createLabel()
            call(invalidPrototype, arrayListOf(F32Value(0.0F)), hashSetOf(), cont)
            switchLabel(cont)
            ret(I32Type, arrayOf(I32Value.of(0)))
        }

        val throwable = assertFails { builder.build() }
        assertTrue { throwable is ValidateSSAErrorException }
    }

    @Test
    fun testMultiplyTerminateInstructions() {
        val builder = ModuleBuilder.create()

        val throwable = assertFails {
            builder.createFunction("main", I32Type, arrayListOf()).apply {
                val header = currentLabel().let {
                    val header = createLabel()
                    branch(header)
                    header
                }

                val label = header.let {
                    switchLabel(header)
                    val label = createLabel()
                    branch(label)
                    ret(I32Type, arrayOf(I32Value.of(0)))
                    label
                }

                label.apply {
                    switchLabel(label)
                    branch(header)
                }
            }
            builder.build()
        }
        assertTrue { throwable is ValidateSSAErrorException }
    }

    @Test
    fun testMultiplyProjections() {
        val builder = ModuleBuilder.create()

        builder.createFunction("main", U32Type, arrayListOf()).apply {
            val tuple = tupleDiv(U32Value.of(100), U32Value.of(20))

            proj(tuple, 0)
            val proj1 = proj(tuple, 0)
            ret(U32Type, arrayOf(proj1))
        }

        val throwable = assertFails { builder.build() }
        assertTrue { throwable is ValidateSSAErrorException }
    }

    @Test
    fun testInconsistentCondBranch() {
        val builder = ModuleBuilder.create()

        builder.createFunction("main", I32Type, arrayListOf(I32Type)).apply {
            val arg = argument(0)
            val cmp = icmp(I32Value.of(0), IntPredicate.Eq, arg)
            val add = add(I32Value.of(0), arg)
            val cont = createLabel()
            val then = createLabel()
            branchCond(cmp, then, cont)
            switchLabel(then).let {
                branch(cont)
            }
            switchLabel(cont).let {
                ret(I32Type, arrayOf(add))
            }
        }

        val throwable = assertFails { builder.build() }
        assertTrue { throwable is ValidateSSAErrorException }
    }

    @Test
    fun testInconsistentFlagToInt() {
        val builder = ModuleBuilder.create()

        builder.createFunction("main", I32Type, arrayListOf(I32Type)).apply {
            val arg = argument(0)
            val cmp = icmp(I32Value.of(0), IntPredicate.Eq, arg)
            val add = add(I32Value.of(0), arg)
            val i = flag2int(cmp, I32Type)
            ret(I32Type, arrayOf(i))
        }

        val throwable = assertFails { builder.build() }
        assertTrue { throwable is ValidateSSAErrorException }
    }

    @Test
    fun testInconsistentSelect() {
        val builder = ModuleBuilder.create()

        builder.createFunction("main", I32Type, arrayListOf(I32Type)).apply {
            val arg = argument(0)
            val cmp = icmp(I32Value.of(0), IntPredicate.Eq, arg)
            val add = add(I32Value.of(0), arg)
            val i = select(cmp, I32Type, I32Value.of(0), add)
            ret(I32Type, arrayOf(i))
        }

        val throwable = assertFails { builder.build() }
        assertTrue { throwable is ValidateSSAErrorException }
    }
}