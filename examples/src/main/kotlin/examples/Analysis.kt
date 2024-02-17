package examples

import ir.FunctionPrototype
import ir.U16Value
import ir.U64Value
import ir.instruction.ArithmeticBinaryOp
import ir.instruction.IntPredicate
import ir.module.builder.impl.ModuleBuilder
import ir.pass.ana.VerifySSA
import ir.pass.transform.Mem2Reg
import ir.pass.transform.Mem2RegFabric
import ir.pass.transform.utils.JoinPointSet
import ir.platform.x64.regalloc.LinearScan
import ir.types.Type

fun main() {
    val builder = ModuleBuilder.create()

    builder.createFunction("hello", Type.U64, arrayListOf(Type.Ptr, Type.Ptr)).apply {
        val arg1 = argument(0)
        val arg2 = argument(1)
        val variable = alloc(Type.U64)
        val variable2 = alloc(Type.U16)

        val v1   = load(Type.U64, arg1)
        val res  = ucmp(U64Value(32), IntPredicate.Gt, v1)
        store(variable, U64Value(12))
        store(variable2, U16Value(14))

        val extraLabel = createLabel()
        branch(extraLabel)
        switchLabel(extraLabel)

        val trueLabel = createLabel()
        val falseLabel = createLabel()
        val mergeLabel = createLabel()
        branchCond(res, trueLabel, falseLabel)

        switchLabel(trueLabel)

        store(variable, U64Value(120))
        val add1 = arithmeticBinary(U16Value(14), ArithmeticBinaryOp.Add, U16Value(14))
        store(variable2, add1)
        branch(mergeLabel)

        switchLabel(falseLabel)
        store(variable, U64Value(15))
        store(variable2, U16Value(16))
        store(arg2, U64Value(19))
        branch(mergeLabel);

        switchLabel(mergeLabel)

        val phi = phi(arrayListOf(U64Value(63), U64Value(43)), arrayListOf(trueLabel, falseLabel))
        val cast = trunc(phi, Type.U16)
        val arithm = arithmeticBinary(U16Value(1337), ArithmeticBinaryOp.Sub, cast)
        val conv = zext(arithm, Type.U64)
        arithmeticBinary(conv, ArithmeticBinaryOp.Sub, phi)

        load(Type.U64, variable)

        val retv = load(Type.U64, variable2)
        ret(retv)
    }

    val module = builder.build()
    
    println(module.toString())

    val helloFn = FunctionPrototype("hello", Type.U64, arrayListOf(Type.Ptr, Type.Ptr))
    val data = module.findFunction(helloFn)
    val cfg = data.blocks

    println("preorder")
    for (bb in cfg.preorder()) {
        print("$bb ")
    }
    println()

    println("bfs traverse")
    for (bb in cfg.bfsTraversal()) {
        print("$bb ")
    }
    println()

    println("postorder")
    for (bb in cfg.postorder()) {
        print("$bb ")
    }
    println()

    println("dominators")
    for ((bb, idom) in cfg.dominatorTree()) {
        print("$bb -> $idom, ")
    }
    println()

    println("dominance frontiers")
    for ((bb, idom) in cfg.dominatorTree().frontiers()) {
        print("$bb -> $idom, ")
    }
    println()

    println("join set")
    for ((bb, idom) in JoinPointSet.evaluate(cfg)) {
        print("$bb -> $idom, ")
    }
    println()

    println(data.liveness())
    val newModule = Mem2RegFabric.create(module).run()
    println(newModule.toString())

    VerifySSA.run(newModule)
    println(data.liveness())
    println(LinearScan.alloc(module.findFunction(helloFn)))
}