package examples

import ir.I32Value
import ir.StringLiteral
import ir.types.Type
import ir.module.builder.ModuleBuilder
import ir.pass.transform.SSADestruction
import ir.platform.x64.CodeEmitter

fun main() {
    val builder = ModuleBuilder.create()
    val helloStr = builder.createGlobalConstant("str", StringLiteral("Hello world"))
    val printf = builder.createExternFunction("printf", Type.I32, arrayListOf(Type.I8.ptr()))
    builder.createFunction("main", Type.Void, arrayListOf()).apply {
        val call = call(printf, arrayListOf(helloStr))
        ret(I32Value(0))
    }

    val module = builder.build()
    println(module.toString())
    val asm = CodeEmitter.codegen(SSADestruction.run(module))
    println(asm.toString())
}