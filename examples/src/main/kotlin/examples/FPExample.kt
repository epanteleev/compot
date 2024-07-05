package examples

import ir.*
import ir.types.Type
import ir.instruction.ArithmeticBinaryOp
import ir.module.builder.impl.ModuleBuilder
import ir.pass.transform.SSADestructionFabric
import ir.platform.common.CodeGenerationFactory
import ir.platform.common.TargetPlatform
import ir.value.F32Value
import ir.value.I32Value


fun main() {
    val builder = ModuleBuilder.create()
    val printFloat = builder.createExternFunction("printFloat", Type.Void, arrayListOf(Type.F32))
    builder.createFunction("main", Type.I32, arrayListOf()).apply {
        val first = alloc(Type.F32)
        store(first, F32Value(4f))
        val second = alloc(Type.F32)
        store(second, F32Value(8f))

        load(Type.F32, first)
        val s = load(Type.F32, second)
        val res = arithmeticBinary(s, ArithmeticBinaryOp.Add, s)
        val cont = createLabel()
        vcall(printFloat, arrayListOf(res), cont)
        switchLabel(cont)
        ret(I32Value(0))
    }

    val module = builder.build()
    println(module.toString())
    val des = SSADestructionFabric.create(module).run()
    println(des)
    val asm = CodeGenerationFactory()
        .setTarget(TargetPlatform.X64)
        .build(des)

    println(asm.toString())
}