package ssa.ir.dominance

import ir.types.Type
import ir.module.Module
import ir.instruction.IntPredicate
import ir.module.SSAModule
import ir.module.block.BlockViewer
import ir.module.builder.impl.ModuleBuilder
import ir.pass.analysis.dominance.PostDominatorTreeFabric
import ir.types.PtrType
import ir.types.U16Type
import ir.value.constant.*

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class PostDominatorTreeTest {
    private fun withBasicBlocks(): SSAModule {
        val moduleBuilder = ModuleBuilder.create()
        val builder = moduleBuilder.createFunction("hello", U16Type, arrayListOf(PtrType))
        val b1 = builder.createLabel()
        builder.branch(b1)

        builder.switchLabel(b1)

        val b2 = builder.createLabel()
        val b3 = builder.createLabel()
        val cmp1 = builder.icmp(
            I32Value.of(12), IntPredicate.Ne,
            I32Value.of(43)
        )
        builder.branchCond(cmp1, b2, b3)

        builder.switchLabel(b3)

        val b4 = builder.createLabel()
        val b5 = builder.createLabel()
        val b6 = builder.createLabel()
        val exit = builder.createLabel()

        builder.branch(b4)
        builder.switchLabel(b4)
        val cmp4 = builder.icmp(
            I32Value.of(152), IntPredicate.Ne,
            I32Value.of(443)
        )
        builder.branchCond(cmp4, b6, b5)

        builder.switchLabel(b6)
        builder.branch(b4)

        builder.switchLabel(b5)
        builder.branch(exit)

        builder.switchLabel(b2)
        builder.branch(exit)

        builder.switchLabel(exit)
        builder.ret(U16Type, arrayOf(U16Value.of(0U)))

        return moduleBuilder.build()
    }

    @Test
    fun testPostDominator() {
        val module = withBasicBlocks()
        val domTree = module.findFunction("hello").analysis(PostDominatorTreeFabric)

        assertTrue(domTree.postDominates(BlockViewer(1), BlockViewer(0)))
        assertTrue(domTree.postDominates(BlockViewer(7), BlockViewer(0)))
        assertTrue(domTree.postDominates(BlockViewer(4), BlockViewer(6)))
        assertTrue(domTree.postDominates(BlockViewer(5), BlockViewer(4)))
        assertTrue(domTree.postDominates(BlockViewer(7), BlockViewer(2)))
        assertFalse { domTree.postDominates(BlockViewer(3), BlockViewer(6)) }
    }
}