package ir.platform

import ir.module.Module
import ir.pass.CompileContext
import ir.platform.x64.codegen.X64CodeGenerator


enum class Target {
    X64
}

interface AnyCodeGenerator {
    fun emit(): CompiledModule
}

class CodeGenerationFactory {
    private var ctx: CompileContext? = null
    private var target: Target? = null

    fun setContext(context: CompileContext): CodeGenerationFactory {
        ctx = context
        return this
    }

    fun setTarget(target: Target): CodeGenerationFactory {
        this.target = target
        return this
    }

    fun build(module: Module): CompiledModule {
        val compiled = when (target as Target) {
            Target.X64 -> X64CodeGenerator(module).emit()
        }

        return compiled
    }
}