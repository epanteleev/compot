package examples

import ir.types.*
import ir.module.builder.impl.ModuleBuilder
import ir.pass.transform.SSADestructionFabric
import ir.pass.transform.SwitchReplacementFabric
import ir.platform.common.CodeGenerationFactory
import ir.platform.common.TargetPlatform
import ir.value.constant.I32Value
import ir.value.constant.U8Value


fun main() {
    val builder = ModuleBuilder.create()
    builder.createFunction("func", I32Type, arrayListOf(I32Type)).apply {
        val default = createLabel()
        val b1 = createLabel()
        val b2 = createLabel()
        val b3 = createLabel()

        val exit = createLabel()

        val targets = listOf(b1, b2, b3)
        val table   = listOf(U8Value.of(1), U8Value.of(2), U8Value.of(3))

        switch(U8Value.of(2), default, table, targets)
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
            ret(I32Type, arrayOf(I32Value.of(0)))
        }
    }

    val module = builder.build()
    println(module.toString())

    val asm = CodeGenerationFactory()
        .setTarget(TargetPlatform.X64)
        .build(SSADestructionFabric.create(SwitchReplacementFabric.create(module).run()).run())

    println(asm.toString())
}