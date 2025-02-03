package examples

import ir.types.VoidType
import ir.module.builder.impl.ModuleBuilder
import ir.types.U64Type


fun main() {
    val moduleBuilder = ModuleBuilder.create()
    val printInt = moduleBuilder.createExternFunction("printInt", VoidType, arrayListOf(U64Type), setOf())
    val argumentTypes = arrayListOf(U64Type, U64Type, U64Type, U64Type, U64Type, U64Type, U64Type, U64Type)

    val builder = moduleBuilder.createFunction("sum8", U64Type, argumentTypes)
    val arg1 = builder.argument(0)
    val arg2 = builder.argument(1)
    val arg3 = builder.argument(2)
    val arg4 = builder.argument(3)
    val arg5 = builder.argument(4)
    val arg6 = builder.argument(5)
    val arg7 = builder.argument(6)
    val arg8 = builder.argument(7)

    val regValue = builder.alloc(U64Type)

    val arg1Alloc = builder.alloc(U64Type)
    val arg2Alloc = builder.alloc(U64Type)
    val arg3Alloc = builder.alloc(U64Type)
    val arg4Alloc = builder.alloc(U64Type)
    val arg5Alloc = builder.alloc(U64Type)
    val arg6Alloc = builder.alloc(U64Type)
    val arg7Alloc = builder.alloc(U64Type)
    val arg8Alloc = builder.alloc(U64Type)

    builder.store(arg1Alloc, arg1)
    builder.store(arg2Alloc, arg2)
    builder.store(arg3Alloc, arg3)
    builder.store(arg4Alloc, arg4)
    builder.store(arg5Alloc, arg5)
    builder.store(arg6Alloc, arg6)
    builder.store(arg7Alloc, arg7)
    builder.store(arg8Alloc, arg8)

    val a = builder.load(U64Type, arg1Alloc)
    val b = builder.load(U64Type, arg2Alloc)
    val add1 = builder.add(a, b)

    val c = builder.load(U64Type, arg3Alloc)
    val add2 = builder.add(add1, c)

    val d = builder.load(U64Type, arg4Alloc)
    val add3 = builder.add(add2, d)

    val e = builder.load(U64Type, arg5Alloc)
    val add4 = builder.add(add3, e)

    val f = builder.load(U64Type, arg6Alloc)
    val add5 = builder.add(add4, f)

    val f1 = builder.load(U64Type, arg7Alloc)
    val add6 = builder.add(add5, f1)

    val f2 = builder.load(U64Type, arg8Alloc)
    val add7 = builder.add(add6, f2)

    val cont = builder.createLabel()
    builder.vcall(printInt, arrayListOf(add7), emptySet(), cont)
    builder.switchLabel(cont)
    builder.store(regValue, add7)
    val ret = builder.load(U64Type, regValue)
    builder.ret(U64Type, arrayOf(ret))

    val module = moduleBuilder.build()
    println(module.toString())
}