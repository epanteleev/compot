package examples

import ir.module.builder.impl.ModuleBuilder
import ir.pass.transform.SSADestructionFabric
import ir.platform.common.CodeGenerationFactory
import ir.platform.common.TargetPlatform
import ir.types.*
import ir.value.constant.*


fun main() {
    val builder = ModuleBuilder.create()
    val printFloat = builder.createExternFunction("printFloat", VoidType, arrayListOf(F32Type), setOf())
    builder.createFunction("main", I32Type, arrayListOf()).apply {
        val first = alloc(F32Type)
        store(first, F32Value(4f))
        val second = alloc(F32Type)
        store(second, F32Value(8f))

        load(F32Type, first)
        val s = load(F32Type, second)
        val res = add(s, s)
        val cont = createLabel()
        vcall(printFloat, arrayListOf(res), emptySet(), cont)
        switchLabel(cont)
        ret(I32Type, arrayOf(I32Value.of(0)))
    }

    val module = builder.build()
    println(module.toString())
    val des = SSADestructionFabric.create(module).run()
    println(des)
    val asm = CodeGenerationFactory()
        .setTarget(TargetPlatform.X64)
        .build(des)

    println(asm.toString())
}