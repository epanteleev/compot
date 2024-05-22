package examples

import ir.I32Value

fun main() {
    val builder = ir.module.builder.impl.ModuleBuilder.create()
    builder.createFunction("func", ir.types.Type.I32, arrayListOf(ir.types.Type.I32, ir.types.Type.VarArgType)).apply {
        val divRes = tupleDiv(I32Value(10), I32Value(2))
        val reminder = proj(divRes, 1)
        ret(reminder)
    }

    val module = builder.build()
    println(module.toString())
    val asm = ir.platform.common.CodeGenerationFactory()
        .setTarget(ir.platform.common.Target.X64)
        .build(ir.pass.transform.SSADestructionFabric.create(module).run())

    println(asm.toString())
}
