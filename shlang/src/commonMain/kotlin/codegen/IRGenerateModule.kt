package codegen

import parser.nodes.*
import ir.module.Module
import ir.module.builder.impl.ModuleBuilder
import ir.pass.analysis.ValidateSSAErrorException
import tokenizer.Position
import typedesc.TypeHolder


data class IRCodeGenError(override val message: String, val position: Position) : Exception(message)

object GenerateIR {
    fun apply(typeHolder: TypeHolder, node: ProgramNode): Module {
        return IRGen.apply(typeHolder, node)
    }
}

private class IRGen private constructor(typeHolder: TypeHolder): AbstractIRGenerator(ModuleBuilder.create(), typeHolder, VarStack(), NameGenerator()) {
    fun visit(programNode: ProgramNode) = varStack.scoped {
        for (node in programNode.nodes) {
            when (node) {
                is FunctionDeclarationNode -> generateFunction(node.function)
                is GlobalDeclaration  -> generateDeclaration(node.declaration)
            }
        }
    }

    private fun generateFunction(node: FunctionNode) = typeHolder.scoped {
        val gen = FunGenInitializer(mb, typeHolder, varStack, nameGenerator)
        gen.generate(node)
    }

    private fun generateDeclaration(node: Declaration) {
        for (declarator in node.nonTypedefDeclarators()) {
            when (declarator) {
                is Declarator     -> generateGlobalDeclarator(declarator)
                is InitDeclarator -> generateGlobalAssignmentDeclarator(declarator)
                else -> throw IRCodeGenError("Unsupported declarator $declarator", node.begin())
            }
        }
    }

    companion object {
        fun apply(typeHolder: TypeHolder, node: ProgramNode): Module {
            val irGen = IRGen(typeHolder)
            irGen.visit(node)
            try {
                return irGen.mb.build()
            } catch (e: ValidateSSAErrorException) {
                println("Error: ${e.message}")
                println("Function:\n${e.functionData}")
                throw e
            }
        }
    }
}