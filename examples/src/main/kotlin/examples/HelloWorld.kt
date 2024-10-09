package examples

import ir.types.*
import ir.global.StringLiteralGlobalConstant
import ir.module.builder.impl.ModuleBuilder
import ir.pass.transform.SSADestructionFabric
import ir.platform.common.CodeGenerationFactory
import ir.platform.common.TargetPlatform
import I32Value


fun main() {
    val builder = ModuleBuilder.create()
    val helloStr = builder.addConstant(StringLiteralGlobalConstant("str", ArrayType(Type.I8, 11),"Hello world"))
    val printf = builder.createExternFunction("printf", Type.I32, arrayListOf(Type.Ptr))
    builder.createFunction("main", Type.I32, arrayListOf()).apply {
        val cont = createLabel()
        call(printf, arrayListOf(helloStr), cont)
        switchLabel(cont)
        ret(Type.I32, arrayOf(I32Value(0)))
    }

    val module = builder.build()
    println(module.toString())

    val asm = CodeGenerationFactory()
        .setTarget(TargetPlatform.X64)
        .build(SSADestructionFabric.create(module).run())

    println(asm.toString())
}