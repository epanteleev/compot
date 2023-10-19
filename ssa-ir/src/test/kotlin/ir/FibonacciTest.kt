package ir

import ir.module.block.BlockViewer
import ir.module.builder.ModuleBuilder
import ir.instruction.ArithmeticBinaryOp
import ir.instruction.IntPredicate
import ir.module.Module
import ir.pass.ana.VerifySSA
import ir.pass.transform.Mem2Reg
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FibonacciTest {
    private fun withBasicBlocks(): Module {
        val moduleBuilder = ModuleBuilder.create()
        val builder = moduleBuilder.createFunction("fib", Type.I32, arrayListOf(Type.I32))

        val n = builder.argument(0)

        val retVal = builder.stackAlloc(Type.I32, 1)
        val nAddr  = builder.stackAlloc(Type.I32, 1)

        val a = builder.stackAlloc(Type.I32, 1)
        val b = builder.stackAlloc(Type.I32, 1)
        val c = builder.stackAlloc(Type.I32, 1)
        val i = builder.stackAlloc(Type.I32, 1)

        builder.store(nAddr, n)
        builder.store(a, I32Value(0))
        builder.store(b, I32Value(1))
        val v0 = builder.load(nAddr)
        val cmp = builder.intCompare(v0, IntPredicate.Eq, I32Value(0))

        val ifThen = builder.createLabel()
        val ifEnd = builder.createLabel()
        val forCond = builder.createLabel()
        val forBody = builder.createLabel()
        val forInc = builder.createLabel()
        val forEnd = builder.createLabel()
        val ret = builder.createLabel()

        builder.branchCond(cmp, ifThen, ifEnd)

        builder.switchLabel(ifThen)

        val v1 = builder.load(a)
        builder.store(retVal, v1)
        builder.branch(ret)

        builder.switchLabel(ifEnd)
        builder.store(i, I32Value(2))
        builder.branch(forCond)

        builder.switchLabel(forCond)
        val v2 = builder.load(i)
        val v3 = builder.load(nAddr)
        val cmp1 = builder.intCompare(v2, IntPredicate.Sle, v3)
        builder.branchCond(cmp1, forBody, forEnd)

        builder.switchLabel(forBody)
        val v4 = builder.load(a)
        val v5 = builder.load(b)
        val add = builder.arithmeticBinary(v4, ArithmeticBinaryOp.Add, v5)
        builder.store(c, add)
        val v6 = builder.load(b)
        builder.store(a, v6)
        val v7 = builder.load(c)
        builder.store(b, v7)
        builder.branch(forInc)

        builder.switchLabel(forInc)
        val v8 = builder.load(i)
        val inc = builder.arithmeticBinary(v8, ArithmeticBinaryOp.Add, I32Value(1))
        builder.store(i, inc)

        builder.branch(forCond)

        builder.switchLabel(forEnd)
        val v9 = builder.load(b)
        builder.store(retVal, v9)
        builder.branch(ret)

        builder.switchLabel(ret)
        val v10 = builder.load(retVal)
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
    fun testCopy() {
        val module = withBasicBlocks()
        val moduleCopy = module.copy()
        assertEquals(module.toString(), moduleCopy.toString())

        val copy = moduleCopy.copy()
        val copyModule2String = copy.toString()
        assertEquals(copy.toString(), copyModule2String)

        val originalMem2Reg = VerifySSA.run(Mem2Reg.run(module))
        val copyMem2Reg     = VerifySSA.run(Mem2Reg.run(module))

        assertEquals(originalMem2Reg.toString(), copyMem2Reg.toString())
        assertEquals(copy.toString(), copyModule2String)
    }
}