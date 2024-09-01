package examples

import ir.types.Type
import ir.module.builder.impl.ModuleBuilder


fun main() {
    val moduleBuilder = ModuleBuilder.create()
    val printInt = moduleBuilder.createExternFunction("printInt", Type.Void, arrayListOf(Type.U64))
    val argumentTypes = arrayListOf(Type.U64, Type.U64, Type.U64, Type.U64, Type.U64, Type.U64, Type.U64, Type.U64)

    val builder = moduleBuilder.createFunction("sum8", Type.U64, argumentTypes)
    val arg1 = builder.argument(0)
    val arg2 = builder.argument(1)
    val arg3 = builder.argument(2)
    val arg4 = builder.argument(3)
    val arg5 = builder.argument(4)
    val arg6 = builder.argument(5)
    val arg7 = builder.argument(6)
    val arg8 = builder.argument(7)

    val regValue = builder.alloc(Type.U64)

    val arg1Alloc = builder.alloc(Type.U64)
    val arg2Alloc = builder.alloc(Type.U64)
    val arg3Alloc = builder.alloc(Type.U64)
    val arg4Alloc = builder.alloc(Type.U64)
    val arg5Alloc = builder.alloc(Type.U64)
    val arg6Alloc = builder.alloc(Type.U64)
    val arg7Alloc = builder.alloc(Type.U64)
    val arg8Alloc = builder.alloc(Type.U64)

    builder.store(arg1Alloc, arg1)
    builder.store(arg2Alloc, arg2)
    builder.store(arg3Alloc, arg3)
    builder.store(arg4Alloc, arg4)
    builder.store(arg5Alloc, arg5)
    builder.store(arg6Alloc, arg6)
    builder.store(arg7Alloc, arg7)
    builder.store(arg8Alloc, arg8)

    val a = builder.load(Type.U64, arg1Alloc)
    val b = builder.load(Type.U64, arg2Alloc)
    val add1 = builder.add(a, b)

    val c = builder.load(Type.U64, arg3Alloc)
    val add2 = builder.add(add1, c)

    val d = builder.load(Type.U64, arg4Alloc)
    val add3 = builder.add(add2, d)

    val e = builder.load(Type.U64, arg5Alloc)
    val add4 = builder.add(add3, e)

    val f = builder.load(Type.U64, arg6Alloc)
    val add5 = builder.add(add4, f)

    val f1 = builder.load(Type.U64, arg7Alloc)
    val add6 = builder.add(add5, f1)

    val f2 = builder.load(Type.U64, arg8Alloc)
    val add7 = builder.add(add6, f2)

    val cont = builder.createLabel()
    builder.vcall(printInt, arrayListOf(add7), cont)
    builder.switchLabel(cont)
    builder.store(regValue, add7)
    val ret = builder.load(Type.U64, regValue)
    builder.ret(Type.U64, arrayOf(ret))

    moduleBuilder.build()
}