package examples

import ir.global.StringLiteralGlobalConstant
import ir.types.*
import ir.module.builder.impl.ModuleBuilder
import ir.pass.CompileContext
import ir.platform.common.CodeGenerationFactory
import ir.platform.common.TargetPlatform
import ir.value.constant.I32Value


fun main() {
    val builder = ModuleBuilder.create()
    val helloStr = builder.addConstant(StringLiteralGlobalConstant("str", ArrayType(I8Type, 10), "Hello world"))
    val printf = builder.createExternFunction("printf", I32Type, arrayListOf(PtrType), setOf())
    builder.createFunction("main", I32Type, arrayListOf(I32Type)).apply {
        val cont = createLabel()
        call(printf, arrayListOf(helloStr, I32Value.of(0)), emptySet(), cont)
        switchLabel(cont)
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