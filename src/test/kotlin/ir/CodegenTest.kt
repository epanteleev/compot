package ir

import ir.builder.ModuleBuilder
import ir.codegen.CodeEmitter
import ir.codegen.LinearScan
import ir.pass.transform.Mem2Reg
import ir.utils.DumpModule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CodegenTest {
    @Test
    fun test() {
        val builder = ModuleBuilder.create()

        val fn = builder.createFunction("sum", Type.U64, arrayListOf(Type.U64, Type.U64))
        val arg1 = builder.argument(0)
        val arg2 = builder.argument(1)

        val regValue = builder.stackAlloc(Type.U64, 1)

        val arg1Alloc = builder.stackAlloc(Type.U64, 1)
        val arg2Alloc = builder.stackAlloc(Type.U64, 1)

        builder.store(arg1Alloc, arg1)
        builder.store(arg2Alloc, arg2)

        val a = builder.load(arg1Alloc)
        val b = builder.load(arg2Alloc)
        val add = builder.arithmeticBinary(a, ArithmeticBinaryOp.Add, b)
        builder.store(regValue, add)

        val ret = builder.load(regValue)
        builder.ret(ret)

        val module = builder.build()

        println(DumpModule.apply(module))
        println(LinearScan.alloc(module.findFunction(fn)))
        println(CodeEmitter.codegen(module))

        Mem2Reg.run(module)
        println(DumpModule.apply(module))
        println(LinearScan.alloc(module.findFunction(fn)))
        println(CodeEmitter.codegen(module))
    }
}