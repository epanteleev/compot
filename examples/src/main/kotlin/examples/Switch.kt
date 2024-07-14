package examples

import ir.types.*
import ir.module.builder.impl.ModuleBuilder
import ir.pass.transform.SSADestructionFabric
import ir.pass.transform.SwitchReplacementFabric
import ir.platform.common.CodeGenerationFactory
import ir.platform.common.TargetPlatform
import ir.value.I32Value
import ir.value.U8Value


fun main() {
    val builder = ModuleBuilder.create()
    builder.createFunction("func", Type.I32, arrayListOf(Type.I32), true).apply {
        val default = createLabel()
        val b1 = createLabel()
        val b2 = createLabel()
        val b3 = createLabel()

        val exit = createLabel()

        val targets = listOf(b1, b2, b3)
        val table   = listOf(U8Value(1), U8Value(2), U8Value(3))

        switch(U8Value(2), default, table, targets)
        switchLabel(b1).let {
            branch(exit)
        }

        switchLabel(b2).let {
            branch(exit)
        }

        switchLabel(b3).let {
            branch(exit)
        }

        switchLabel(default).let {
            branch(exit)
        }

        switchLabel(exit).let {
            ret(I32Value(0))
        }
    }

    val module = builder.build()
    println(module.toString())

    val asm = CodeGenerationFactory()
        .setTarget(TargetPlatform.X64)
        .build(SSADestructionFabric.create(SwitchReplacementFabric.create(module).run()).run())

    println(asm.toString())
}