import ir.*
import ir.builder.ModuleBuilder
import ir.codegen.LinearScan
import ir.pass.ana.VerifySSA
import ir.pass.transform.Mem2Reg
import ir.utils.DumpModule
import ir.utils.JoinPointSet

fun main(args: Array<String>) {
    val builder = ModuleBuilder.create()

    val fn = builder.createFunction("hello", Type.U64, arrayListOf(Type.U16.ptr(), Type.U64.ptr()))
    val arg1 = builder.argument(0)
    val arg2 = builder.argument(1)
    val variable = builder.stackAlloc(Type.U64, 1)
    val variable2 = builder.stackAlloc(Type.U16, 1)

    val v1 = builder.load(arg1)
    val ttt  = builder.cast(v1, Type.U64, CastType.SignExtend)
    val res  = builder.intCompare(U64Value(32), IntPredicate.Sgt, ttt)
    builder.store(variable, U64Value(12))
    builder.store(variable2, U16Value(14))

    val extraLabel = builder.createLabel()
    builder.branch(extraLabel)
    builder.switchLabel(extraLabel)

    val trueLabel = builder.createLabel()
    val falseLabel = builder.createLabel()
    val mergeLabel = builder.createLabel()
    builder.branchCond(res, trueLabel, falseLabel)

    builder.switchLabel(trueLabel)

    builder.store(variable, U64Value(120))
    val add1 = builder.arithmeticBinary(U16Value(14), ArithmeticBinaryOp.Add, U16Value(14))
    builder.store(variable2, add1)
    builder.branch(mergeLabel)

    builder.switchLabel(falseLabel)
    builder.store(variable, U64Value(15))
    builder.store(variable2, U16Value(16))
    builder.store(arg2, U64Value(19))
    builder.branch(mergeLabel);

    builder.switchLabel(mergeLabel)

    val phi = builder.phi(arrayListOf(U64Value(63), U64Value(43)), arrayListOf(trueLabel, falseLabel))
    val cast = builder.cast(phi, Type.U16, CastType.Truncate)
    val arithm = builder.arithmeticBinary(U16Value(1337), ArithmeticBinaryOp.Sub, cast)
    val conv = builder.cast(arithm, Type.U64, CastType.ZeroExtend)
    val sum1 = builder.arithmeticBinary(conv, ArithmeticBinaryOp.Sub, phi)

    val v5 = builder.load(variable)

    val retv = builder.load(variable2)
    builder.ret(retv)

    val module = builder.build()
    
    println(DumpModule.apply(module))

    val data = module.findFunction(fn)
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
    println(DumpModule.apply(newModule))

    VerifySSA.run(newModule)
    println(data.liveness())
    println(LinearScan.alloc(module.findFunction(fn)))
}