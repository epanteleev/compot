package examples

import ir.I32Value
import ir.StringLiteralGlobal
import ir.module.builder.impl.ModuleBuilder
import ir.pass.transform.SSADestruction
import ir.platform.x64.codegen.CodeEmitter
import ir.types.ArrayType
import ir.types.Type

fun main() {
    val builder = ModuleBuilder.create()
    val helloStr = builder.addConstant(StringLiteralGlobal("str", ArrayType(Type.I8, 11),"Hello world"))
    val printf = builder.createExternFunction("printf", Type.I32, arrayListOf(Type.I8.ptr()))
    builder.createFunction("main", Type.I32, arrayListOf()).apply {
        call(printf, arrayListOf(helloStr))
        ret(I32Value(0))
    }

    val module = builder.build()
    println(module.toString())
    val asm = CodeEmitter.codegen(SSADestruction.run(module))
    println(asm.toString())
}