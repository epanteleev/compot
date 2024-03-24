package ssa.ir

import ir.FunctionPrototype
import ir.I32Value
import ir.instruction.ArithmeticBinaryOp
import ir.instruction.IntPredicate
import ir.module.Module
import ir.module.block.BlockViewer
import ir.module.block.Label
import ir.module.builder.impl.ModuleBuilder
import ir.pass.ana.LoopBlockData
import ir.pass.ana.LoopDetection
import ir.pass.ana.VerifySSA
import ir.pass.transform.Mem2RegFabric
import ir.types.Type
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FibonacciTest {
    private fun withBasicBlocks(): Module {
        val moduleBuilder = ModuleBuilder.create()
        val builder = moduleBuilder.createFunction("fib", Type.I32, arrayListOf(Type.I32))

        val n = builder.argument(0)

        val retVal = builder.alloc(Type.I32)
        val nAddr  = builder.alloc(Type.I32)

        val a = builder.alloc(Type.I32)
        val b = builder.alloc(Type.I32)
        val c = builder.alloc(Type.I32)
        val i = builder.alloc(Type.I32)

        builder.store(nAddr, n)
        builder.store(a, I32Value(0))
        builder.store(b, I32Value(1))
        val v0 = builder.load(Type.I32, nAddr)
        val cmp = builder.icmp(v0, IntPredicate.Eq, I32Value(0))

        val ifThen = builder.createLabel()
        val ifEnd = builder.createLabel()
        val forCond = builder.createLabel()
        val forBody = builder.createLabel()
        val forInc = builder.createLabel()
        val forEnd = builder.createLabel()
        val ret = builder.createLabel()

        builder.branchCond(cmp, ifThen, ifEnd)

        builder.switchLabel(ifThen)

        val v1 = builder.load(Type.I32, a)
        builder.store(retVal, v1)
        builder.branch(ret)

        builder.switchLabel(ifEnd)
        builder.store(i, I32Value(2))
        builder.branch(forCond)

        builder.switchLabel(forCond)
        val v2 = builder.load(Type.I32, i)
        val v3 = builder.load(Type.I32, nAddr)
        val cmp1 = builder.icmp(v2, IntPredicate.Le, v3)
        builder.branchCond(cmp1, forBody, forEnd)

        builder.switchLabel(forBody)
        val v4 = builder.load(Type.I32, a)
        val v5 = builder.load(Type.I32, b)
        val add = builder.arithmeticBinary(v4, ArithmeticBinaryOp.Add, v5)
        builder.store(c, add)
        val v6 = builder.load(Type.I32, b)
        builder.store(a, v6)
        val v7 = builder.load(Type.I32, c)
        builder.store(b, v7)
        builder.branch(forInc)

        builder.switchLabel(forInc)
        val v8 = builder.load(Type.I32, i)
        val inc = builder.arithmeticBinary(v8, ArithmeticBinaryOp.Add, I32Value(1))
        builder.store(i, inc)

        builder.branch(forCond)

        builder.switchLabel(forEnd)
        val v9 = builder.load(Type.I32, b)
        builder.store(retVal, v9)
        builder.branch(ret)

        builder.switchLabel(ret)
        val v10 = builder.load(Type.I32, retVal)
        builder.ret(v10)

        val module = moduleBuilder.build()
        VerifySSA.run(module)

        return module
    }

    @Test
    fun testDominator() {
        val module = withBasicBlocks()
        val prototype = FunctionPrototype("fib", Type.I32, arrayListOf(Type.I32))
        val cfg = module.findFunction(prototype).blocks
        val domTree = cfg.dominatorTree()

        assertTrue(domTree.dominates(BlockViewer(0), BlockViewer(7)))
        assertTrue(domTree.dominates(BlockViewer(0), BlockViewer(1)))
        assertTrue(domTree.dominates(BlockViewer(0), BlockViewer(2)))
        assertTrue(domTree.dominates(BlockViewer(3), BlockViewer(6)))
        assertTrue(domTree.dominates(BlockViewer(3), BlockViewer(4)))
        assertTrue(domTree.dominates(BlockViewer(4), BlockViewer(5)))
    }

    @Test
    fun testLoopDetection() {
        val module = withBasicBlocks()
        println(module)
        val prototype = FunctionPrototype("fib", Type.I32, arrayListOf(Type.I32))
        val cfg = module.findFunction(prototype)
        val loopInfo = LoopDetection.evaluate(cfg.blocks)

        assertEquals(1, loopInfo.headers().size)

        val loopData = loopInfo.get(BlockViewer(3))
        assertNotNull(loopData)
        assertEquals(BlockViewer(6), loopData.exit() as Label)
        assertEquals(BlockViewer(4), loopData.enter() as Label)
    }

    @Test
    fun testCopy() {
        val module = withBasicBlocks()
        val moduleCopy = module.copy()
        assertEquals(module.toString(), moduleCopy.toString())

        val copy = moduleCopy.copy()
        val copyModule2String = copy.toString()
        assertEquals(copy.toString(), copyModule2String)

        val originalMem2Reg = VerifySSA.run(Mem2RegFabric.create(module).run())
        val copyMem2Reg     = VerifySSA.run(Mem2RegFabric.create(module).run())

        assertEquals(originalMem2Reg.toString(), copyMem2Reg.toString())
        assertEquals(copy.toString(), copyModule2String)
    }
}