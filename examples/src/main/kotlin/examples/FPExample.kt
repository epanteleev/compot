package examples

import ir.*
import ir.types.Type
import ir.pass.transform.SSADestruction
import ir.instruction.ArithmeticBinaryOp
import ir.module.builder.impl.ModuleBuilder
import ir.pass.transform.SSADestructionFabric
import ir.platform.x64.codegen.x64CodeGenerator


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
        vcall(printFloat, arrayListOf(res))
        ret(I32Value(0))
    }

    val module = builder.build()
    println(module.toString())
    val des = SSADestructionFabric.create(module).run()
    println(des)
    val asm = x64CodeGenerator.emit(des)
    println(asm.toString())
}