package ir.pass.common

import ir.module.Module


abstract class TransformPass(protected val module: Module) {
    abstract fun name(): String
    abstract fun run(): Module
}