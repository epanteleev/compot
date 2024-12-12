package examples

import ir.types.Type
import I32Value
import ir.platform.common.CodeGenerationFactory
import ir.platform.common.TargetPlatform
import ir.types.U32Type


fun main() {
    val builder = ir.module.builder.impl.ModuleBuilder.create()
    builder.createFunction("func", I32Type, arrayListOf(I32Type)).apply {
        val divRes = tupleDiv(I32Value(10), I32Value(2))
        val reminder = proj(divRes, 1)
        ret(U32Type, arrayOf(reminder))
    }

    val module = builder.build()
    println(module.toString())
    val asm = CodeGenerationFactory()
        .setTarget(TargetPlatform.X64)
        .build(ir.pass.transform.SSADestructionFabric.create(module).run())

    println(asm.toString())
}
