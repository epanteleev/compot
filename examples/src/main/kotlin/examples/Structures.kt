package examples

import ir.*
import ir.instruction.ArithmeticBinaryOp
import ir.module.builder.ModuleBuilder
import ir.pass.transform.SSADestruction
import ir.platform.x64.codegen.CodeEmitter
import ir.types.StructType
import ir.types.Type

fun main() {
    val builder = ModuleBuilder.create()
    val printFloat = builder.createExternFunction("printInt", Type.Void, arrayListOf(Type.I32))
    val pointStruct = StructType("point", arrayListOf(Type.I32, Type.I32))
    builder.addStructType(pointStruct)

    builder.createFunction("main", Type.I32, arrayListOf()).apply {
        val first = alloc(pointStruct)
        val firstField = gep(first, Type.I32, I32Value(0))
        store(firstField, I32Value(4))
        ret(I32Value(0))
    }

    val module = builder.build()
    println(module.toString())
    val des = SSADestruction.run(module)
    println(des)
    val asm = CodeEmitter.codegen(des)
    println(asm.toString())
}