package ir.platform.common

import ir.module.Module
import ir.pass.CompileContext
import ir.platform.x64.codegen.X64CodeGenerator


enum class TargetPlatform {
    X64
}

interface AnyCodeGenerator {
    fun emit(): CompiledModule
}

class CodeGenerationFactory {
    private var ctx: CompileContext? = null
    private var target: TargetPlatform? = null

    fun setContext(context: CompileContext): CodeGenerationFactory {
        ctx = context
        return this
    }

    fun setTarget(target: TargetPlatform): CodeGenerationFactory {
        this.target = target
        return this
    }

    fun build(module: Module): CompiledModule {
        val compiled = when (target as TargetPlatform) {
            TargetPlatform.X64 -> X64CodeGenerator(module).emit()
        }

        return compiled
    }
}