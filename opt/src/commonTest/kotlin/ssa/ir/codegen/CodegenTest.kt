package ssa.ir.codegen

import asm.x64.GPRegister.*
import ir.module.builder.impl.ModuleBuilder
import ir.pass.CompileContext
import ir.pass.transform.Mem2RegFabric
import ir.platform.x64.pass.analysis.regalloc.VirtualRegistersPool
import ir.types.Type
import ir.types.U64Type
import ir.types.VoidType
import kotlin.test.Test
import kotlin.test.assertEquals

class CodegenTest {
    @Test
    fun test() {
        val moduleBuilder = ModuleBuilder.create()

        val builder = moduleBuilder.createFunction("sum", U64Type, arrayListOf(U64Type, U64Type))
        val arg1 = builder.argument(0)
        val arg2 = builder.argument(1)

        val retValue = builder.alloc(U64Type)

        val arg1Alloc = builder.alloc(U64Type)
        val arg2Alloc = builder.alloc(U64Type)

        builder.store(arg1Alloc, arg1)
        builder.store(arg2Alloc, arg2)

        val a = builder.load(U64Type, arg1Alloc)
        val b = builder.load(U64Type, arg2Alloc)
        val add = builder.add(a, b)

        val printInt = moduleBuilder.createExternFunction("printInt", VoidType, arrayListOf(U64Type), setOf())
        val cont = builder.createLabel()
        builder.vcall(printInt, arrayListOf(add), hashSetOf(), cont)
        builder.switchLabel(cont)

        builder.store(retValue, add)

        val ret = builder.load(U64Type, retValue)
        builder.ret(U64Type, arrayOf(ret))

         val module = moduleBuilder.build()

        //println(DumpModule.apply(module))
        //println(LinearScan.alloc(module.findFunction(prototype)))
        //println(CodeEmitter.codegen(module))

        val ctx = CompileContext.empty()
        Mem2RegFabric.create(module, ctx).run()
        //println(DumpModule.apply(module))
        //println(LinearScan.alloc(module.findFunction(prototype)))
        //println(CodeEmitter.codegen(module))

        //asserts
        val fn = module.findFunction("sum")
        val pool = VirtualRegistersPool.create(fn.arguments())
        assertEquals(pool.arguments()[0], rdi)
        assertEquals(pool.arguments()[1], rsi)
    }
}