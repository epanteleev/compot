package ir

import ir.block.BlockViewer
import ir.builder.ModuleBuilder
import ir.instruction.ArithmeticBinaryOp
import ir.instruction.CastType
import ir.instruction.IntPredicate
import ir.pass.ana.VerifySSA
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CFGTraversalTest {

    private fun withBasicBlocks(): BasicBlocks {
        val moduleBuilder = ModuleBuilder.create()
        val prototype = FunctionPrototype("hello", Type.U16, arrayListOf(Type.U16.ptr(), Type.U64.ptr(), Type.U64.ptr()))
        val builder = moduleBuilder.createFunction("hello", Type.U16, arrayListOf(Type.U16.ptr(), Type.U64.ptr(), Type.U64.ptr()))
        val arg1 = builder.argument(0)
        val arg2 = builder.argument(1)
        val arg3 = builder.argument(2)
        val v1 = builder.load(arg1)
        val ttt  = builder.cast(v1, Type.U64, CastType.SignExtend)
        val res  = builder.intCompare(U64Value(32), IntPredicate.Sgt, ttt)

        val trueLabel = builder.createLabel()
        val falseLabel = builder.createLabel()
        val mergeLabel = builder.createLabel()
        builder.branchCond(res, trueLabel, falseLabel)

        builder.switchLabel(trueLabel)
        builder.store(arg3, U64Value(12))
        builder.branch(mergeLabel)

        builder.switchLabel(falseLabel)
        builder.store(arg2, U64Value(19))
        builder.branch(mergeLabel)

        builder.switchLabel(mergeLabel)
        val arithm = builder.arithmeticBinary(U16Value(1337), ArithmeticBinaryOp.Sub, U16Value(64))
        builder.ret(arithm)

        val module = moduleBuilder.build()
        VerifySSA.run(module)
        return module.findFunction(prototype).blocks
    }

    @Test
    fun testTraversePreorder() {
        val expected = listOf(0, 1, 2, 3)

        for ((idx, bb) in withBasicBlocks().preorder().withIndex()) {
            assertEquals(expected[idx], bb.index)
        }
    }

    @Test
    fun testTraversePostorder() {
        val expected = listOf(3, 2, 1, 0)

        for ((idx, bb) in withBasicBlocks().postorder().withIndex()) {
            assertEquals(expected[idx], bb.index)
        }
    }

    @Test
    fun testDominator() {
        val domTree = withBasicBlocks().dominatorTree()

        assertTrue(domTree.dominates(BlockViewer(0), BlockViewer(1)))
        assertTrue(domTree.dominates(BlockViewer(0), BlockViewer(2)))
        assertTrue(domTree.dominates(BlockViewer(0), BlockViewer(3)))
    }
}