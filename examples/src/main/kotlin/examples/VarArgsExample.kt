package examples

import ir.I32Value
import ir.StringLiteralGlobal
import ir.module.builder.impl.ModuleBuilder
import ir.pass.transform.SSADestructionFabric
import ir.platform.x64.codegen.x64CodeGenerator
import ir.types.ArrayType
import ir.types.Type

fun main() {
    val builder = ModuleBuilder.create()
    val helloStr = builder.addConstant(StringLiteralGlobal("str", ArrayType(Type.I8, 11), "Hello world"))
    val printf = builder.createExternFunction("printf", Type.I32, arrayListOf(Type.Ptr, Type.VarArgType))
    builder.createFunction("main", Type.I32, arrayListOf(Type.I32, Type.VarArgType)).apply {
        call(printf, arrayListOf(helloStr))
        ret(I32Value(0))
    }

    val module = builder.build()
    println(module.toString())
    val asm = x64CodeGenerator.emit(SSADestructionFabric.create(module).run())
    println(asm.toString())
}
