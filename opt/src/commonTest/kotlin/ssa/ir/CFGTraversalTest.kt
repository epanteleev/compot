package ssa.ir

import ir.value.constant.*
import ir.instruction.IntPredicate
import ir.module.FunctionData
import ir.module.block.BlockViewer
import ir.module.builder.impl.ModuleBuilder
import ir.pass.analysis.VerifySSA
import ir.pass.analysis.dominance.DominatorTreeFabric
import ir.pass.analysis.traverse.PostOrderFabric
import ir.pass.analysis.traverse.PreOrderFabric
import ir.types.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CFGTraversalTest {

    private fun withBasicBlocks(): FunctionData {
        val moduleBuilder = ModuleBuilder.create()
        val builder = moduleBuilder.createFunction("hello", U16Type, arrayListOf(PtrType, PtrType, PtrType))
        val arg1 = builder.argument(0)
        val arg2 = builder.argument(1)
        val arg3 = builder.argument(2)
        val v1 = builder.load(I16Type, arg1)
        val ttt  = builder.sext(v1, I64Type)
        val res  = builder.icmp(I64Value.of(32), IntPredicate.Gt, ttt)

        val trueLabel = builder.createLabel()
        val falseLabel = builder.createLabel()
        val mergeLabel = builder.createLabel()
        builder.branchCond(res, trueLabel, falseLabel)

        builder.switchLabel(trueLabel)
        builder.store(arg3, I64Value.of(12))
        builder.branch(mergeLabel)

        builder.switchLabel(falseLabel)
        builder.store(arg2, I64Value.of(19))
        builder.branch(mergeLabel)

        builder.switchLabel(mergeLabel)
        val arithm = builder.sub(U16Value.of(1337U), U16Value.of(64U))
        builder.ret(U16Type, arrayOf(arithm))

        val module = moduleBuilder.build()
        VerifySSA.run(module)
        return module.findFunction("hello")
    }

    @Test
    fun testTraversePreorder() {
        val expected = listOf(0, 1, 2, 3)

        for ((idx, bb) in withBasicBlocks().analysis(PreOrderFabric).withIndex()) {
            assertEquals(expected[idx], bb.index)
        }
    }

    @Test
    fun testTraversePostorder() {
        val expected = listOf(3, 2, 1, 0)

        for ((idx, bb) in withBasicBlocks().analysis(PostOrderFabric).withIndex()) {
            assertEquals(expected[idx], bb.index)
        }
    }

    @Test
    fun testDominator() {
        val domTree = withBasicBlocks().analysis(DominatorTreeFabric)

        assertTrue(domTree.dominates(BlockViewer(0), BlockViewer(1)))
        assertTrue(domTree.dominates(BlockViewer(0), BlockViewer(2)))
        assertTrue(domTree.dominates(BlockViewer(0), BlockViewer(3)))
    }
}