package codegen

import parser.nodes.*
import ir.module.Module
import ir.module.builder.impl.ModuleBuilder
import ir.pass.analysis.ValidateSSAErrorException
import ir.value.Value
import parser.LineAgnosticAstPrinter
import tokenizer.Position
import typedesc.TypeHolder


data class IRCodeGenError(override val message: String, val position: Position) : Exception(message)

class IRGen private constructor(typeHolder: TypeHolder): AbstractIRGenerator(ModuleBuilder.create(), typeHolder, VarStack<Value>(), NameGenerator()) {
    fun visit(programNode: ProgramNode) = varStack.scoped {
        for (node in programNode.nodes) {
            when (node) {
                is FunctionNode -> generateFunction(node)
                is Declaration  -> generateDeclaration(node)
                else -> throw IRCodeGenError("Function expected", node.begin())
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