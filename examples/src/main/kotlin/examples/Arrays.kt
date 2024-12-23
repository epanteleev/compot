package examples

import ir.types.*
import ir.module.builder.impl.ModuleBuilder
import ir.pass.analysis.LivenessAnalysisPassFabric
import ir.pass.analysis.VerifySSA
import ir.pass.transform.Mem2RegFabric
import ir.value.constant.*


fun main() {
    val builder = ModuleBuilder.create()
    val proto = builder.createExternFunction("printIntArray", VoidType, arrayListOf(PtrType, I32Type))

    builder.createFunction("main", U64Type, arrayListOf()).apply {
        val array = alloc(ArrayType(I32Type, 5))
        val e0 = gep(array, I32Type, I32Value.of(0))
        store(e0, I32Value.of(0))

        val e1 = gep(array, I32Type, I32Value(1))
        store(e1, I32Value(1))

        val e2 = gep(array, I32Type, I32Value(2))
        store(e2, I32Value(2))

        val e3 = gep(array, I32Type, I32Value(3))
        store(e3, I32Value(3))

        val e4 = gep(array, I32Type, I32Value(4))
        store(e4, I32Value(4))

        val arrPtr = gep(array, I32Type, I32Value(0))
        val cont = createLabel()
        vcall(proto, arrayListOf(arrPtr, I32Value(5)), setOf(), cont)
        switchLabel(cont)
        ret(U64Type, arrayOf(U64Value(0)))
    }

    val module = builder.build()

    println(module.toString())

    val data = module.findFunction("main")

    println(data.analysis(LivenessAnalysisPassFabric))
    val newModule = Mem2RegFabric.create(module).run()
    println(newModule.toString())

    VerifySSA.run(newModule)
    println(data.analysis(LivenessAnalysisPassFabric))
}