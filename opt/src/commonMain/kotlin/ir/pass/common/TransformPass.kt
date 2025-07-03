package ir.pass.common

import ir.module.Module
import ir.pass.CompileContext


abstract class TransformPass<M: Module<*>>(protected val module: M, protected val ctx: CompileContext) {
    abstract fun name(): String
    abstract fun run(): M
}