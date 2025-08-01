package examples

import ir.types.*
import ir.module.builder.impl.ModuleBuilder
import ir.pass.CompileContext
import ir.pass.transform.SSADestructionFabric
import ir.platform.common.CodeGenerationFactory
import ir.platform.common.TargetPlatform
import ir.value.constant.*


fun main() {
    val builder = ModuleBuilder.create()
    val pointStruct = builder.structType("point", arrayListOf(I32Type, I32Type))

    builder.createFunction("main", I32Type, arrayListOf()).apply {
        val first = alloc(pointStruct)
        val firstField = gfp(first, pointStruct, I64Value.of(0))
        store(firstField, I32Value.of(4))
        ret(I32Type, arrayOf(I32Value.of(0)))
    }

    val module = builder.build()
    println(module.toString())
    val ctx = CompileContext.empty()
    val asm = CodeGenerationFactory()
        .setTarget(TargetPlatform.X64)
        .setContext(ctx)
        .build(module)

    println(asm.toString())
}