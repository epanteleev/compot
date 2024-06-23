package examples

import ir.*
import ir.global.StringLiteralGlobal
import ir.types.*
import ir.module.builder.impl.ModuleBuilder
import ir.pass.transform.SSADestructionFabric
import ir.platform.common.CodeGenerationFactory
import ir.platform.common.TargetPlatform


fun main() {
    val builder = ModuleBuilder.create()
    val helloStr = builder.addConstant(StringLiteralGlobal("str", ArrayType(Type.I8, 10), "Hello world"))
    val printf = builder.createExternFunction("printf", Type.I32, arrayListOf(Type.Ptr, Type.VarArgType))
    builder.createFunction("main", Type.I32, arrayListOf(Type.I32, Type.VarArgType)).apply {
        val cont = createLabel()
        call(printf, arrayListOf(helloStr, I32Value(0)), cont)
        switchLabel(cont)
        ret(I32Value(0))
    }

    val module = builder.build()
    println(module.toString())
    val asm = CodeGenerationFactory()
        .setTarget(TargetPlatform.X64)
        .build(SSADestructionFabric.create(module).run())

    println(asm.toString())
}
