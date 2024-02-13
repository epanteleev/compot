package ir.pass

import ir.module.Module


interface PassFabric {
    fun create(module: Module): TransformPass
}