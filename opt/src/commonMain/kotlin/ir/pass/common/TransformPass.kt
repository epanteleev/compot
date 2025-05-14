package ir.pass.common

import ir.module.Module
import ir.pass.CompileContext


abstract class TransformPass(protected val module: Module, protected val ctx: CompileContext) {
    abstract fun name(): String
    abstract fun run(): Module
}