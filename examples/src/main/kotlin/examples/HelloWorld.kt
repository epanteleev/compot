package examples

import ir.types.*
import ir.global.StringLiteralGlobalConstant
import ir.module.builder.impl.ModuleBuilder
import ir.pass.CompileContext
import ir.pass.transform.SSADestructionFabric
import ir.platform.common.CodeGenerationFactory
import ir.platform.common.TargetPlatform
import ir.value.constant.I32Value


fun main() {
    val builder = ModuleBuilder.create()
    val helloStr = builder.addConstant(StringLiteralGlobalConstant("str", ArrayType(I8Type, 11),"Hello world"))
    val printf = builder.createExternFunction("printf", I32Type, arrayListOf(PtrType), setOf())
    builder.createFunction("main", I32Type, arrayListOf()).apply {
        val cont = createLabel()
        call(printf, arrayListOf(helloStr), emptySet(), cont)
        switchLabel(cont)
        ret(I32Type, arrayOf(I32Value.of(0)))
    }

    val module = builder.build()
    println(module.toString())

    val asm = CodeGenerationFactory()
        .setTarget(TargetPlatform.X64)
        .setContext(CompileContext.empty())
        .build(module)

    println(asm.toString())
}