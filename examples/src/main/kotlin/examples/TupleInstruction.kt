package examples

import ir.pass.CompileContext
import ir.pass.transform.SSADestructionFabric
import ir.platform.common.CodeGenerationFactory
import ir.platform.common.TargetPlatform
import ir.types.I32Type
import ir.types.U32Type
import ir.value.constant.I32Value


fun main() {
    val builder = ir.module.builder.impl.ModuleBuilder.create()
    builder.createFunction("func", I32Type, arrayListOf(I32Type)).apply {
        val divRes = tupleDiv(I32Value.of(10), I32Value.of(2))
        ret(U32Type, arrayOf(divRes.remainder))
    }

    val module = builder.build()
    println(module.toString())

    val ctx = CompileContext.empty()
    val asm = CodeGenerationFactory()
        .setTarget(TargetPlatform.X64)
        .build(SSADestructionFabric.create(module, ctx).run())

    println(asm.toString())
}
