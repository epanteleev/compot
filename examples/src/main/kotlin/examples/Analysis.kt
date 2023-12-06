package examples

import ir.*
import ir.module.builder.ModuleBuilder
import ir.platform.regalloc.LinearScan
import ir.instruction.ArithmeticBinaryOp
import ir.instruction.CastType
import ir.instruction.IntPredicate
import ir.pass.ana.VerifySSA
import ir.pass.transform.Mem2Reg
import ir.pass.transform.utils.JoinPointSet
import ir.types.Type

fun main() {
    val builder = ModuleBuilder.create()

    builder.createFunction("hello", Type.U64, arrayListOf(Type.U16.ptr(), Type.U64.ptr())).apply {
        val d = argument(0)
        val arg1 = argument(0)
        val arg2 = argument(1)
        val variable = stackAlloc(Type.U64)
        val variable2 = stackAlloc(Type.U16)

        val v1   = load(Type.U64, arg1)
        val ttt  = cast(v1, Type.U64, CastType.SignExtend)
        val res  = intCompare(U64Value(32), IntPredicate.Sgt, ttt)
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
        val cast = cast(phi, Type.U16, CastType.Truncate)
        val arithm = arithmeticBinary(U16Value(1337), ArithmeticBinaryOp.Sub, cast)
        val conv = cast(arithm, Type.U64, CastType.ZeroExtend)
        val sum1 = arithmeticBinary(conv, ArithmeticBinaryOp.Sub, phi)

        val v5 = load(Type.U64, variable)

        val retv = load(Type.U64, variable2)
        ret(retv)
    }

    val module = builder.build()
    
    println(module.toString())

    val helloFn = FunctionPrototype("hello", Type.U64, arrayListOf(Type.U16.ptr(), Type.U64.ptr()))
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
    val newModule = Mem2Reg.run(module)
    println(newModule.toString())

    VerifySSA.run(newModule)
    println(data.liveness())
    println(LinearScan.alloc(module.findFunction(helloFn)))
}