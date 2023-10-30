package ir.codegen

import ir.*
import asm.x64.*
import ir.types.Type
import ir.pass.transform.Mem2Reg
import ir.module.builder.ModuleBuilder
import ir.instruction.ArithmeticBinaryOp
import ir.platform.regalloc.VirtualRegistersPool

import kotlin.test.Test
import kotlin.test.assertEquals

class CodegenTest {
    @Test
    fun test() {
        val moduleBuilder = ModuleBuilder.create()

        val prototype = FunctionPrototype("sum", Type.U64, arrayListOf(Type.U64, Type.U64))
        val builder = moduleBuilder.createFunction("sum", Type.U64, arrayListOf(Type.U64, Type.U64))
        val arg1 = builder.argument(0)
        val arg2 = builder.argument(1)

        val retValue = builder.stackAlloc(Type.U64)

        val arg1Alloc = builder.stackAlloc(Type.U64)
        val arg2Alloc = builder.stackAlloc(Type.U64)

        builder.store(arg1Alloc, arg1)
        builder.store(arg2Alloc, arg2)

        val a = builder.load(arg1Alloc)
        val b = builder.load(arg2Alloc)
        val add = builder.arithmeticBinary(a, ArithmeticBinaryOp.Add, b)

        val printInt = moduleBuilder.createExternFunction("printInt", Type.Void, arrayListOf(Type.U64))
        builder.vcall(printInt, arrayListOf(add))

        builder.store(retValue, add)

        val ret = builder.load(retValue)
        builder.ret(ret)

        val module = moduleBuilder.build()

        //println(DumpModule.apply(module))
        //println(LinearScan.alloc(module.findFunction(prototype)))
        //println(CodeEmitter.codegen(module))

        Mem2Reg.run(module)
        //println(DumpModule.apply(module))
        //println(LinearScan.alloc(module.findFunction(prototype)))
        //println(CodeEmitter.codegen(module))

        //asserts
        val pool = VirtualRegistersPool.create(module.functions[0].arguments())
        assertEquals(pool.arguments()[0], Rdi.rdi)
        assertEquals(pool.arguments()[1], Rsi.rsi)
        assertEquals(pool.allocSlot(retValue), Mem.mem(Rbp.rbp, -8, 8))
    }
}